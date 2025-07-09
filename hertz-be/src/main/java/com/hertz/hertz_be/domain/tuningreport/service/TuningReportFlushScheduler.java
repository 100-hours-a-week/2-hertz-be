package com.hertz.hertz_be.domain.tuningreport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    @Scheduled(cron = "0 0/30 * * * *")
    public void flushDirtyReports() {
        Set<String> dirty = cacheManager.getDirtyReportIds();
        if (dirty.isEmpty()) return;

        Set<String> pages = cacheManager.scanPageKeys();

        for (String pageKey : pages) {
            for (String rid : dirty) {
                if (!redisTemplate.opsForHash().hasKey(pageKey, rid)) continue;
                try {
                    String json = (String) redisTemplate.opsForHash().get(pageKey, rid);

                    // ✅ Jackson이 내부 static class도 잘 deserialize 가능하므로 아래처럼 사용해도 문제 없음
                    TuningReportListResponse.ReportItem item =
                            objectMapper.readValue(json, TuningReportListResponse.ReportItem.class);

                    Long reportId = Long.valueOf(rid);
                    TuningReport report = reportRepo.findById(reportId)
                            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

                    // ✅ 반응 수 반영 (getReactions())
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
                                if (reacted) {
                                    if (!reactionRepo.existsByReportIdAndUserIdAndReactionType(
                                            reportId, userId, type)) {
                                        reactionRepo.save(
                                                TuningReportUserReaction.builder()
                                                        .report(report)
                                                        .user(User.of(userId))
                                                        .reactionType(type)
                                                        .build()
                                        );
                                    }
                                } else {
                                    reactionRepo.deleteByReportIdAndUserIdAndReactionType(
                                            reportId, userId, type);
                                }
                            }
                        }
                    }

                    cacheManager.clearDirtyReportId(rid);
                    log.info("[FLUSHED] reportId={} synchronized.", reportId);
                } catch (Exception e) {
                    log.warn("[FLUSH FAILED] reportId={} error: {}", rid, e.getMessage());
                }
            }
        }
    }
}
