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
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
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
            value = {
                    ObjectOptimisticLockingFailureException.class,
                    DeadlockLoserDataAccessException.class,
                    CannotAcquireLockException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public TuningReportReactionResponse toggleReportReaction(Long userId, Long reportId, TuningReportReactionToggleRequest request) {

        // 먼저 PESSIMISTIC_WRITE 락 걸고 가져옴
        TuningReport report = tuningReportRepository.findWithLockById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트가 존재하지 않습니다."));

        ReactionType reactionType = request.reactionType();
        boolean isReacted;

        boolean alreadyExists = tuningReportUserReactionRepository.existsByReportIdAndUserIdAndReactionType(
                reportId, userId, reactionType);

        if (alreadyExists) {
            // 수치 먼저 감소 후 삭제
            report.decreaseReaction(reactionType);
            tuningReportUserReactionRepository.deleteByReportIdAndUserIdAndReactionType(reportId, userId, reactionType);
            isReacted = false;
        } else {
            // 저장 후 수치 증가
            TuningReportUserReaction reaction = TuningReportUserReaction.builder()
                    .report(report)
                    .user(User.of(userId))
                    .reactionType(reactionType)
                    .build();

            tuningReportUserReactionRepository.save(reaction);
            report.increaseReaction(reactionType);
            isReacted = true;
        }

        int reactionCount = switch (reactionType) {
            case CELEBRATE -> report.getReactionCelebrate();
            case THUMBS_UP -> report.getReactionThumbsUp();
            case LAUGH -> report.getReactionLaugh();
            case EYES -> report.getReactionEyes();
            case HEART -> report.getReactionHeart();
        };

        return new TuningReportReactionResponse(reportId, reactionType, isReacted, reactionCount);
    }
}
