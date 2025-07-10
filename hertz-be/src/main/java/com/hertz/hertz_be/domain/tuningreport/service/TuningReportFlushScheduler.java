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
    @Scheduled(cron = "0 0/2 * * * *")
    public void flushDirtyReports() {
        Set<String> dirty = cacheManager.getDirtyReportIds();
        if (dirty.isEmpty()) return;

        for (String rid : dirty) {
            try {
                Long reportId = Long.valueOf(rid);
                String reportKey = cacheManager.reportItemKey(reportId);
                String json = redisTemplate.opsForValue().get(reportKey);

                if (json == null) continue;

                TuningReportListResponse.ReportItem item =
                        objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);

                TuningReport report = reportRepo.findById(reportId)
                        .orElseThrow(() -> new IllegalArgumentException("Report not found"));

                // ✅ 반응 수 반영
                report.updateReactionsFrom(item.getReactions());
                reportRepo.save(report);

                // ✅ 유저별 반응 동기화
                String userPattern = String.format("reports:%d:user:*", reportId);
                Set<String> userKeys = redisTemplate.keys(userPattern);

                for (String uk : userKeys) {
                    Long userId = Long.valueOf(uk.split(":")[3]);
                    for (ReactionType type : ReactionType.values()) {
                        Boolean reacted = cacheManager.getUserReaction(reportId, userId, type);
                        if (reacted != null) {
                            boolean exists = reactionRepo.existsByReportIdAndUserIdAndReactionType(reportId, userId, type);
                            if (reacted) {
                                if (!exists) {
                                    reactionRepo.save(
                                            TuningReportUserReaction.builder()
                                                    .report(report)
                                                    .user(User.of(userId))
                                                    .reactionType(type)
                                                    .build()
                                    );
                                }
                            } else {
                                if (exists) {
                                    reactionRepo.deleteByReportIdAndUserIdAndReactionType(reportId, userId, type);
                                }
                            }
                        }
                    }
                }

                cacheManager.clearDirtyReportId(rid);
                log.info("✅ [FLUSHED] reportId={} synchronized successfully.", reportId);

            } catch (Exception e) {
                log.warn("❌ [FLUSH FAILED] reportId={} error: {}", rid, e.getMessage());
            }
        }
    }


}
