package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class TuningReportTransactionalService {

    private final TuningReportRepository tuningReportRepository;
    private final TuningReportUserReactionRepository tuningReportUserReactionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TuningReportListResponse fetchDirectlyFromDB(Long userId, int page, int size, TuningReportSortType sort) {
        String domain = userRepository.findDistinctEmailDomains(userId);
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TuningReport> reports = sort.fetch(pageRequest, tuningReportRepository, domain);

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

    public TuningReportListResponse.ReportItem toReportItemWithoutReactions(TuningReport report) {
        return new TuningReportListResponse.ReportItem(
                report.getCreatedAt(), report.getId(), report.getTitle(), report.getContent(),
                new TuningReportListResponse.Reactions (
                        report.getReactionCelebrate(), report.getReactionThumbsUp(),
                        report.getReactionLaugh(), report.getReactionEyes(), report.getReactionHeart()
                ),
                null
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
