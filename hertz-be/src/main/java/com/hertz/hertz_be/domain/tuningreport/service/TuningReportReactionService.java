package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.request.TuningReportReactionToggleRequest;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TuningReportReactionService {

    private final TuningReportRepository tuningReportRepository;
    private final TuningReportUserReactionRepository tuningReportUserReactionRepository;

    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3
    )
    public TuningReportReactionResponse toggleReportReaction(Long userId, Long reportId, TuningReportReactionToggleRequest request) {
        TuningReport report = tuningReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트가 존재하지 않습니다."));

        ReactionType reactionType = request.reactionType();
        boolean isReacted;

        if(tuningReportUserReactionRepository.existsByReportIdAndUserIdAndReactionType(reportId, userId, reactionType)) { // 이미 선택한 리액션인지 확인
            tuningReportUserReactionRepository.deleteByReportIdAndUserIdAndReactionType(reportId, userId, reactionType);
            report.decreaseReaction(reactionType);
            isReacted = false;
        } else {
            TuningReportUserReaction reaction = TuningReportUserReaction.builder()
                    .report(report)
                    .user(User.of(userId))
                    .reactionType(request.reactionType())
                    .build();

            tuningReportUserReactionRepository.save(reaction);
            report.increaseReaction(reactionType);
            isReacted = true;
        }

        int reactionCount = switch(reactionType) {
            case CELEBRATE  -> report.getReactionCelebrate();
            case EYES       -> report.getReactionEyes();
            case HEART      -> report.getReactionHeart();
            case LAUGH      -> report.getReactionLaugh();
            case THUMBS_UP  -> report.getReactionThumbsUp();
        };

        return new TuningReportReactionResponse (reportId, reactionType, isReacted, reactionCount);

    }
}
