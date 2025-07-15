package com.hertz.hertz_be.domain.tuningreport.service;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class WarmupTuningReport {

    private final TuningReportCacheManager cacheManager;
    private final RedisTemplate<String, String> redisTemplate;
    private final TuningReportTransactionalService transactionalService;
    private final TuningReportRepository tuningReportRepository;
    private final TuningReportFlushScheduler tuningReportFlushScheduler;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 15 16 ? * TUE")
    public void warmupKakaotechReports() {
        String domain = "example.com";
        String key = cacheManager.pageListKey(domain);
        String lockKey = "lock:warmup:" + domain;

        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!acquired) {
                return;
            }

            if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
                log.info("🔁 기존 캐시 존재 → flush + invalidate 수행: {}", key);
                tuningReportFlushScheduler.flushDirtyReports();
                cacheManager.invalidateDomainCache(domain);
            }

            log.info("🔥 캐시 워밍업 시작: {}", domain);
            List<TuningReportListResponse.ReportItem> items =
                    TuningReportSortType.LATEST.fetch(PageRequest.of(0, 10),
                                    tuningReportRepository, domain)
                            .stream()
                            .map(transactionalService::toReportItemWithoutReactions)
                            .toList();

            cacheManager.cacheReportList(domain, items);
            log.info("✅ 캐시 워밍업 완료: {}건", items.size());

        } catch (Exception e) {
            log.error("❌ 워밍업 실패: domain={}, message={}", domain, e.getMessage(), e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
