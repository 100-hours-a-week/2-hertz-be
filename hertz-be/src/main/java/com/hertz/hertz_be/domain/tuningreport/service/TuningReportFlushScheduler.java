package com.hertz.hertz_be.domain.tuningreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReportUserReaction;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportUserReactionRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class TuningReportFlushScheduler {
    private final TuningReportCacheManager cacheManager;
    private final TuningReportRepository reportRepo;
    private final TuningReportUserReactionRepository reactionRepo;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Transactional
    @Scheduled(cron = "0 0/3 * * * *")
    public void flushDirtyReports() {
        Set<String> dirty = cacheManager.getDirtyReportIds();
        if (dirty.isEmpty()) return;

        for (String rid : dirty) {
            Long reportId = null;
            try {
                reportId = Long.valueOf(rid);
                String reportKey = cacheManager.reportItemKey(reportId);
                String json = redisTemplate.opsForValue().get(reportKey);

                if (json == null) {
                    log.warn("❌ [FLUSH SKIP] reportId={} 캐시에 JSON 없음", rid);
                    continue;
                }

                TuningReportListResponse.ReportItem item = null;
                try {
                    item = objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);
                } catch (Exception e) {
                    log.warn("❌ [JSON 파싱 실패] reportId={} msg={}", rid, e.getMessage());
                    continue;
                }

                if (item == null || item.getReactions() == null) {
                    log.warn("❌ [FLUSH SKIP] reportId={} 역직렬화된 객체가 null", rid);
                    continue;
                }

                TuningReport report = reportRepo.findById(reportId)
                        .orElseThrow(() -> new IllegalArgumentException("Report not found"));

                // 각 게시글의 반응 수 동기화
                report.updateReactionsFrom(item.getReactions());
                reportRepo.save(report);

                // 각 게시글에 대한 유저별 반응 동기화
                String userPattern = String.format("reports:%d:user:*", reportId);
                Set<String> userKeys = redisTemplate.keys(userPattern);

                for (String uk : userKeys) {
                    try {
                        Long userId = Long.parseLong(uk.split(":")[3]);

                        for (ReactionType type : ReactionType.values()) {
                            Boolean reacted = cacheManager.getUserReaction(reportId, userId, type);
                            if (reacted == null) continue;

                            boolean exists = reactionRepo.existsByReportIdAndUserIdAndReactionType(reportId, userId, type);

                            if (reacted && !exists) {
                                reactionRepo.save(TuningReportUserReaction.builder()
                                        .report(report)
                                        .user(User.of(userId))
                                        .reactionType(type)
                                        .build());
                            } else if (!reacted && exists) {
                                reactionRepo.deleteByReportIdAndUserIdAndReactionType(reportId, userId, type);
                            }
                        }

                    } catch (Exception e) {
                        log.warn("❌ [유저 반응 동기화 실패] userKey={} msg={}", uk, e.getMessage());
                    }
                }

                log.info("✅ [FLUSHED] reportId={} synchronized successfully.", reportId);

            } catch (Exception e) {
                log.warn("❌ [FLUSH FAILED] reportId={} error: {}", rid, e.getMessage());
            } finally {
                if (reportId != null) {
                    try {
                        cacheManager.clearDirtyReportId(rid);
                    } catch (Exception e) {
                        log.error("❗ dirty set에서 제거 실패: reportId={} error={}", rid, e.getMessage());
                    }
                }
            }
        }
    }


}
