package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportService {

    private final TuningReportTransactionalService transactionalService;
    private final TuningReportCacheManager cacheManager;

    public TuningReportListResponse getReportList(Long userId, int page, int size, TuningReportSortType sort) {
        if (isCacheApplicable(page, size, sort)) {
            List<TuningReportListResponse.ReportItem> cachedItems = loadOrCacheReports(page, size, sort);
            List<TuningReportListResponse.ReportItem> enriched = enrichWithUserReactions(cachedItems, userId);
            return createResponse(enriched, page, size);
        }

        return transactionalService.fetchDirectlyFromDB(userId, page, size, sort);
    }

    private boolean isCacheApplicable(int page, int size, TuningReportSortType sort) {
        return page == 0 && size == 10 && sort == TuningReportSortType.LATEST;
    }

    private List<TuningReportListResponse.ReportItem> loadOrCacheReports(int page, int size, TuningReportSortType sort) {
        List<TuningReportListResponse.ReportItem> items = cacheManager.getCachedReportList();
        if (items == null) {
            log.info("게시글 리스트 반환 시 캐싱 hit ⚠️");
            var pageReq = PageRequest.of(page, size);
            var reports = sort.fetch(pageReq, transactionalService.getTuningReportRepository());
            items = reports.stream()
                    .map(transactionalService::toReportItemWithoutReactions)
                    .collect(Collectors.toList());
            cacheManager.cacheReportList(items);
        }
        log.info("캐싱 되기 위해 load된 item 수: {}", items.size());
        return items;
    }

    private List<TuningReportListResponse.ReportItem> enrichWithUserReactions(List<TuningReportListResponse.ReportItem> items, Long userId) {
        List<Long> reportIds = items.stream()
                .map(TuningReportListResponse.ReportItem::getReportId)
                .toList();

        List<TuningReportUserReaction> dbList =
                transactionalService.getTuningReportUserReactionRepository().findAllByUserIdAndReportIdIn(userId, reportIds);

        log.info("💡 사용자 {}에 대해 DB에서 조회된 반응 수: {}", userId, dbList.size());

        dbList.forEach(reaction ->
                cacheManager.setUserReaction(
                        reaction.getReport().getId(),
                        userId,
                        reaction.getReactionType(),
                        true
                )
        );

        Map<Long, Set<ReactionType>> userReactionMap = dbList.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReport().getId(),
                        Collectors.mapping(TuningReportUserReaction::getReactionType, Collectors.toSet())
                ));

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
