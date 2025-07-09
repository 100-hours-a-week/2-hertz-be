package com.hertz.hertz_be.domain.tuningreport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.tuningreport.dto.request.TuningReportReactionToggleRequest;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TuningReportReactionService {

    private final TuningReportCacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final TuningReportRepository reportRepo;
    private final TuningReportUserReactionRepository reactionRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TuningReportReactionResponse toggleReportReaction(
            Long userId,
            Long reportId,
            TuningReportReactionToggleRequest req
    ) {
        ReactionType type = req.reactionType();

        if (cacheManager.isReportCached(reportId)) {
            return toggleWithCache(reportId, userId, type);
        } else {
            return toggleWithDbFallback(reportId, userId, type);
        }
    }

    // 캐시 기반 처리
    private TuningReportReactionResponse toggleWithCache(Long reportId, Long userId, ReactionType type) {
        boolean isReacted;
        String pageKey = cacheManager.pageKey();
        String userKey = cacheManager.userKey(reportId, userId);

        Boolean current = cacheManager.getUserReaction(reportId, userId, type);
        boolean already = current != null && current;
        isReacted = !already;

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations ops) throws DataAccessException {
                ops.multi();
                ops.opsForHash().increment(pageKey, type.name(), isReacted ? 1L : -1L);
                ops.opsForHash().put(userKey, type.name(), isReacted ? "1" : "0");
                ops.opsForSet().add("dirty:reports", reportId.toString());
                ops.expire(pageKey, Duration.ofMinutes(35));
                ops.expire("dirty:reports", Duration.ofMinutes(35));
                return ops.exec();
            }
        });

        try {
            String json = redisTemplate.opsForHash().get(pageKey, reportId.toString()).toString();
            if (json != null) {
                TuningReportListResponse.ReportItem item = objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);
                if (isReacted) item.getReactions().increase(type);
                else item.getReactions().decrease(type);
                redisTemplate.opsForHash().put(pageKey, reportId.toString(), objectMapper.writeValueAsString(item));
            }
        } catch (JsonProcessingException ignored) {}

        Object cnt = redisTemplate.opsForHash().get(pageKey, type.name());
        int newCount = 0;
        if (cnt instanceof String s) newCount = Integer.parseInt(s);
        else if (cnt instanceof Integer i) newCount = i;

        return new TuningReportReactionResponse(reportId, type, isReacted, newCount);
    }

    // DB 기반 처리
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
    protected TuningReportReactionResponse toggleWithDbFallback(Long reportId, Long userId, ReactionType type) {
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
        return new TuningReportReactionResponse(reportId, type, isReacted, count);
    }
}
