package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportService {

    private final TuningReportRepository tuningReportRepository;
    private final TuningReportUserReactionRepository tuningReportUserReactionRepository;
    private final TuningReportCacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional(readOnly = true)
    public TuningReportListResponse getReportList(Long userId, int page, int size, TuningReportSortType sort) {
        if (isCacheApplicable(page, size, sort)) {
            List<TuningReportListResponse.ReportItem> cachedItems = loadOrCacheReports(page, size, sort);

            List<TuningReportListResponse.ReportItem> enriched = enrichWithUserReactions(cachedItems, userId);

            return createResponse(enriched, page, size);
        }

        return fetchDirectlyFromDB(userId, page, size, sort);
    }

    private boolean isCacheApplicable(int page, int size, TuningReportSortType sort) {
        return page == 0 && size == 10 && sort == TuningReportSortType.LATEST;
    }

    private List<TuningReportListResponse.ReportItem> loadOrCacheReports(int page, int size, TuningReportSortType sort) {
        List<TuningReportListResponse.ReportItem> items = cacheManager.getCachedReportList();
        if (items == null) {
            log.info("게시글 리스트 반환 시 캐싱 hit ⚠️");
            var pageReq = PageRequest.of(page, size);
            var reports = sort.fetch(pageReq, tuningReportRepository);
            items = reports.stream()
                    .map(this::toReportItemWithoutReactions)
                    .collect(Collectors.toList());
            cacheManager.cacheReportList(items);
        }
        log.info("캐싱 되기 위해 load된 item 수: {}", items.size());
        return items;
    }

    private TuningReportListResponse.ReportItem toReportItemWithoutReactions(TuningReport report) {
        return new TuningReportListResponse.ReportItem(
                report.getCreatedAt(), report.getId(), report.getTitle(), report.getContent(),
                new TuningReportListResponse.Reactions (
                        report.getReactionCelebrate(), report.getReactionThumbsUp(),
                        report.getReactionLaugh(), report.getReactionEyes(), report.getReactionHeart()
                ),
                null
        );
    }

    private List<TuningReportListResponse.ReportItem> enrichWithUserReactions(List<TuningReportListResponse.ReportItem> items, Long userId) {
        List<Long> reportIds = items.stream()
                .map(TuningReportListResponse.ReportItem::getReportId)
                .toList();

        List<TuningReportUserReaction> dbList =
                tuningReportUserReactionRepository.findAllByUserIdAndReportIdIn(userId, reportIds);

        log.info("💡 사용자 {}에 대해 DB에서 조회된 반응 수: {}", userId, dbList.size());

        // 캐싱 먼저 수행
        dbList.forEach(reaction ->
                cacheManager.setUserReaction(
                        reaction.getReport().getId(),
                        userId,
                        reaction.getReactionType(),
                        true
                )
        );

        // 사용자별 반응 목록 맵핑
        Map<Long, Set<ReactionType>> userReactionMap = dbList.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReport().getId(),
                        Collectors.mapping(TuningReportUserReaction::getReactionType, Collectors.toSet())
                ));

        // 최종 응답 객체 변환
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

    private TuningReportListResponse fetchDirectlyFromDB(Long userId, int page, int size, TuningReportSortType sort) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TuningReport> reports = sort.fetch(pageRequest, tuningReportRepository);

        List<Long> reportIds = reports.stream().map(TuningReport::getId).toList();
        List<TuningReportUserReaction> userReactions =
                tuningReportUserReactionRepository.findAllByUserIdAndReportIdIn(userId, reportIds);

        Map<Long, Set<ReactionType>> userReactionMap = userReactions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReport().getId(),
                        Collectors.mapping(TuningReportUserReaction::getReactionType, Collectors.toSet())
                ));

        List<TuningReportListResponse.ReportItem> reportItems = reports.stream()
                .map(report -> toReportItemWithReactions(report, userReactionMap.getOrDefault(report.getId(), Set.of())))
                .collect(Collectors.toList());

        log.info("DB에서 바로 반환된 item 수: {}", reportItems.size());
        return new TuningReportListResponse(
                reportItems,
                reports.getNumber(),
                reports.getSize(),
                reports.isLast()
        );
    }

    private TuningReportListResponse.ReportItem toReportItemWithReactions(TuningReport report, Set<ReactionType> myReactions) {
        return new TuningReportListResponse.ReportItem(
                report.getCreatedAt(),
                report.getId(),
                report.getTitle(),
                report.getContent(),
                new TuningReportListResponse.Reactions(
                        report.getReactionCelebrate(),
                        report.getReactionThumbsUp(),
                        report.getReactionLaugh(),
                        report.getReactionEyes(),
                        report.getReactionHeart()
                ),
                new TuningReportListResponse.MyReactions(
                        myReactions.contains(ReactionType.CELEBRATE),
                        myReactions.contains(ReactionType.THUMBS_UP),
                        myReactions.contains(ReactionType.LAUGH),
                        myReactions.contains(ReactionType.EYES),
                        myReactions.contains(ReactionType.HEART)
                )
        );
    }


}
