package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportReactionTransactionalService {

    private final TuningReportRepository reportRepo;
    private final TuningReportUserReactionRepository reactionRepo;

    @Transactional
    @Retryable(
            value = {
                    ObjectOptimisticLockingFailureException.class,
                    DeadlockLoserDataAccessException.class,
                    CannotAcquireLockException.class,
                    DataAccessException.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public TuningReportReactionResponse toggleWithDbFallback(Long reportId, Long userId, ReactionType type) {
        boolean isReacted;
        TuningReport report = reportRepo.findWithLockById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트가 존재하지 않습니다."));

        boolean exists = reactionRepo.existsByReportIdAndUserIdAndReactionType(reportId, userId, type);
        if (exists) {
            report.decreaseReaction(type);
            reactionRepo.deleteByReportIdAndUserIdAndReactionType(reportId, userId, type);
            isReacted = false;
        } else {
            reactionRepo.save(
                    TuningReportUserReaction.builder()
                            .report(report)
                            .user(User.of(userId))
                            .reactionType(type)
                            .build()
            );
            report.increaseReaction(type);
            isReacted = true;
        }

        int count = report.getCountByType(type);
        log.warn("⭐️게시글{}에 반응 처리 완료, DB에 바로 반영", reportId);
        return new TuningReportReactionResponse(reportId, type, isReacted, count);
    }
}
