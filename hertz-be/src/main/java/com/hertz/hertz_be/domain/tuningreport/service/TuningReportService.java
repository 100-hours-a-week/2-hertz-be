package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportService {

    private final TuningReportTransactionalService transactionalService;
    private final TuningReportCacheManager cacheManager;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;

    public TuningReportListResponse getReportList(Long userId, int page, int size, TuningReportSortType sort) {
        if (isCacheApplicable(page, size, sort)) {
            String domain = cacheManager.getUserDomain(userId, userRepository::findDistinctEmailDomains);
            List<TuningReportListResponse.ReportItem> cachedItems = loadOrCacheReports(domain, page, size, sort);
            List<TuningReportListResponse.ReportItem> enriched = enrichWithUserReactions(cachedItems, userId);
            return createResponse(enriched, page, size);
        }

        return transactionalService.fetchDirectlyFromDB(userId, page, size, sort);
    }

    private boolean isCacheApplicable(int page, int size, TuningReportSortType sort) {
        return page == 0 && size == 10 && sort == TuningReportSortType.LATEST;
    }

    private List<TuningReportListResponse.ReportItem> loadOrCacheReports(String domain, int page, int size, TuningReportSortType sort) {
        List<TuningReportListResponse.ReportItem> items = cacheManager.getCachedReportList(domain);

        if (items != null) return items;

        // 캐시 미스 발생 → 락 획득 시도
        String lockKey = "lock:report:domain:" + domain;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("⚠️ 도메인 {} 캐싱 락 획득 실패 → fallback to 캐시 재확인", domain);
                return cacheManager.getCachedReportList(domain);
            }

            // 락 획득 후 다시 한 번 캐시 확인 (동시성 보장)
            items = cacheManager.getCachedReportList(domain);
            if (items == null) {
                log.info("🎯 캐시 미스 → DB 조회 및 캐싱 시작: domain={}", domain);
                var pageReq = PageRequest.of(page, size);
                var reports = sort.fetch(pageReq, transactionalService.getTuningReportRepository(), domain);
                items = reports.stream()
                        .map(transactionalService::toReportItemWithoutReactions)
                        .collect(Collectors.toList());

                cacheManager.cacheReportList(domain, items);
                log.info("✅ DB 조회 완료 및 캐싱: {}건", items.size());
            }

        } catch (InterruptedException e) {
            log.warn("❌ 캐시 락 처리 중단: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return items;
    }

    private List<TuningReportListResponse.ReportItem> enrichWithUserReactions(
            List<TuningReportListResponse.ReportItem> items, Long userId) {

        List<Long> reportIds = items.stream()
                .map(TuningReportListResponse.ReportItem::getReportId)
                .toList();

        boolean anyCached = reportIds.stream()
                .anyMatch(reportId -> cacheManager.hasUserReactionCached(reportId, userId));

        Map<Long, Set<ReactionType>> userReactionMap;

        if (anyCached) {
            userReactionMap = reportIds.stream().collect(Collectors.toMap(
                    reportId -> reportId,
                    reportId -> Arrays.stream(ReactionType.values())
                            .filter(type -> Boolean.TRUE.equals(cacheManager.getUserReaction(reportId, userId, type)))
                            .collect(Collectors.toSet())
            ));
            log.info("✅ Redis에서 사용자 {}의 반응 정보 조회 완료 (DB 미조회)", userId);
        } else {
            List<TuningReportUserReaction> dbList =
                    transactionalService.getTuningReportUserReactionRepository().findAllByUserIdAndReportIdIn(userId, reportIds);

            dbList.forEach(reaction ->
                    cacheManager.setUserReaction(
                            reaction.getReport().getId(),
                            userId,
                            reaction.getReactionType(),
                            true
                    )
            );

            userReactionMap = dbList.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getReport().getId(),
                            Collectors.mapping(TuningReportUserReaction::getReactionType, Collectors.toSet())
                    ));
        }

        return items.stream()
                .map(item -> {
                    Set<ReactionType> reactions = userReactionMap.getOrDefault(item.getReportId(), Set.of());
                    return new TuningReportListResponse.ReportItem(
                            item.getCreatedDate(),
                            item.getReportId(),
                            item.getTitle(),
                            item.getContent(),
                            item.getReactions(),
                            new TuningReportListResponse.MyReactions(
                                    reactions.contains(ReactionType.CELEBRATE),
                                    reactions.contains(ReactionType.THUMBS_UP),
                                    reactions.contains(ReactionType.LAUGH),
                                    reactions.contains(ReactionType.EYES),
                                    reactions.contains(ReactionType.HEART)
                            )
                    );
                })
                .toList();
    }

    private TuningReportListResponse createResponse(List<TuningReportListResponse.ReportItem> items, int page, int size) {
        boolean isLast = items.size() < size;
        return new TuningReportListResponse(items, page, size, isLast);
    }
}
