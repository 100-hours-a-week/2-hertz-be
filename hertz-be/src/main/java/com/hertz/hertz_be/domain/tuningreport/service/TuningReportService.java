package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TuningReportService {

    private final TuningReportRepository tuningReportRepository;
    private final TuningReportUserReactionRepository tuningReportUserReactionRepository;

    @Transactional(readOnly = true)
    public TuningReportListResponse getReportList(Long userId, int page, int size, TuningReportSortType sort) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TuningReport> reports = sort.fetch(pageRequest, tuningReportRepository); // 정렬 타입에 따라

        List<Long> reportIds = reports.stream()
                .map(TuningReport::getId)
                .toList();

        List<TuningReportUserReaction> userReactions = tuningReportUserReactionRepository
                .findAllByUserIdAndReportIdIn(userId, reportIds);

        Map<Long, Set<ReactionType>> userReactionMap = userReactions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getReport().getId(),
                        Collectors.mapping(TuningReportUserReaction::getReactionType, Collectors.toSet())
                ));

        List<TuningReportListResponse.ReportItem> reportItems = reports.stream()
                .map(report -> {
                    Set<ReactionType> myReaction = userReactionMap.getOrDefault(report.getId(), Set.of());

                    return new TuningReportListResponse.ReportItem(
                            report.getCreatedAt(),
                            report.getId(),
                            report.getTitle(),
                            report.getContent(),
                            new TuningReportListResponse.ReportItem.Reactions(
                                    report.getReactionCelebrate(),
                                    report.getReactionThumbsUp(),
                                    report.getReactionLaugh(),
                                    report.getReactionEyes(),
                                    report.getReactionHeart()
                            ),
                            new TuningReportListResponse.ReportItem.MyReactions(
                                    myReaction.contains(ReactionType.CELEBRATE),
                                    myReaction.contains(ReactionType.THUMBS_UP),
                                    myReaction.contains(ReactionType.LAUGH),
                                    myReaction.contains(ReactionType.EYES),
                                    myReaction.contains(ReactionType.HEART)
                            )
                    );
                }).toList();

        return new TuningReportListResponse(
            reportItems,
            reports.getNumber(),
            reports.getSize(),
            reports.isLast()
        );
    }

}
