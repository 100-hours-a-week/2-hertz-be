package com.hertz.hertz_be.domain.tuningreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hertz.hertz_be.domain.tuningreport.dto.request.TuningReportReactionToggleRequest;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TuningReportReactionService {

    private final TuningReportCacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final TuningReportReactionTransactionalService txService;
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
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
        String domain = cacheManager.getUserDomain(userId, userRepository::findDistinctEmailDomains);

        RLock lock = redissonClient.getLock("lock:report:" + reportId);

        int newCount = 0;

        try {
            // 분산 락 획득 (최대 대기 2초, 락 유지 시간 5초)
            boolean locked = lock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("⚠️ 리포트 락 획득 실패: reportId=" + reportId);
            }

            // Step 1: 유저 상태 읽기 및 토글 여부 결정
            Boolean current = cacheManager.getUserReaction(reportId, userId, type);
            boolean already = current != null && current;
            isReacted = !already;

            // Step 2: 유저 상태 + Dirty Set 저장
            redisTemplate.opsForHash().put(userKey, type.name(), isReacted ? "1" : "0");
            cacheManager.markDirty(reportId);

            // Step 3: ReportItem 캐시 읽고 수정
            String json = redisTemplate.opsForValue().get(reportKey);
            if (json != null) {
                TuningReportListResponse.ReportItem item = objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);

                if (isReacted) item.getReactions().increase(type);
                else item.getReactions().decrease(type);

                if (item.getMyReactions() == null)
                    item.setMyReactions(new TuningReportListResponse.MyReactions());
                item.getMyReactions().set(type, isReacted);

                newCount = switch (type) {
                    case CELEBRATE -> item.getReactions().getCelebrate();
                    case THUMBS_UP -> item.getReactions().getThumbsUp();
                    case LAUGH -> item.getReactions().getLaugh();
                    case EYES -> item.getReactions().getEyes();
                    case HEART -> item.getReactions().getHeart();
                };
                log.warn("🔄{}에 반응 처리완료", reportId);
            }

            // 해당 도메인과 해당 사용자와 관련 모든 캐시된 데이터 TTL 갱신
            cacheManager.refreshTuningReportTTL(userId, domain);
            log.warn("🔄모든 캐시된 데이터 TTL 갱신");

        } catch (Exception e) {
            log.warn("❌ 분산 락 처리 실패: reportId={}, error={}", reportId, e.getMessage());
            throw new RuntimeException("toggle 실패", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return new TuningReportReactionResponse(reportId, type, isReacted, newCount);
    }
}
