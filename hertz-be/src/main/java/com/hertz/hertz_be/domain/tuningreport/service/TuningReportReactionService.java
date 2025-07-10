package com.hertz.hertz_be.domain.tuningreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hertz.hertz_be.domain.tuningreport.dto.request.TuningReportReactionToggleRequest;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportReactionService {

    private final TuningReportCacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final TuningReportReactionTransactionalService txService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public TuningReportReactionResponse toggleReportReaction(
            Long userId,
            Long reportId,
            TuningReportReactionToggleRequest req
    ) {
        ReactionType type = req.reactionType();

        if (cacheManager.isReportCached(reportId)) {
            return toggleWithCache(reportId, userId, type);
        } else {
            return txService.toggleWithDbFallback(reportId, userId, type);
        }
    }

    private TuningReportReactionResponse toggleWithCache(Long reportId, Long userId, ReactionType type) {
        boolean isReacted;
        String reportKey = cacheManager.reportItemKey(reportId);
        String userKey = cacheManager.userKey(reportId, userId);

        Boolean current = cacheManager.getUserReaction(reportId, userId, type);
        boolean already = current != null && current;
        isReacted = !already;

        // Step 1: 트랜잭션으로 사용자 반응 상태 + dirty set 저장
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations ops) {
                ops.multi();
                ops.opsForHash().put(userKey, type.name(), isReacted ? "1" : "0");
                ops.opsForSet().add("dirty:reports", reportId.toString());
                ops.expire(userKey, Duration.ofMinutes(35));
                ops.expire("dirty:reports", Duration.ofMinutes(35));
                return ops.exec();
            }
        });

        int newCount = 0;

        // Step 2: ReportItem 캐시 수정 (reactions + myReactions)
        try {
            String json = redisTemplate.opsForValue().get(reportKey);
            if (json != null) {
                TuningReportListResponse.ReportItem item = objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);

                if (isReacted) item.getReactions().increase(type);
                else item.getReactions().decrease(type);

                if (item.getMyReactions() == null)
                    item.setMyReactions(new TuningReportListResponse.MyReactions());
                item.getMyReactions().set(type, isReacted);

                redisTemplate.opsForValue().set(reportKey, objectMapper.writeValueAsString(item), Duration.ofMinutes(35));

                newCount = switch (type) {
                    case CELEBRATE -> item.getReactions().getCelebrate();
                    case THUMBS_UP -> item.getReactions().getThumbsUp();
                    case LAUGH -> item.getReactions().getLaugh();
                    case EYES -> item.getReactions().getEyes();
                    case HEART -> item.getReactions().getHeart();
                };
            }
        } catch (Exception e) {
            log.warn("❌ ReportItem 캐시 갱신 실패: reportId={}, error={}", reportId, e.getMessage());
        }

        return new TuningReportReactionResponse(reportId, type, isReacted, newCount);
    }
}
