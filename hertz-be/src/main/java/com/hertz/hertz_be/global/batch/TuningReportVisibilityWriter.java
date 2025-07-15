package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportCacheManager;
import com.hertz.hertz_be.domain.tuningreport.repository.TuningReportRepository;
import com.hertz.hertz_be.domain.tuningreport.service.TuningReportFlushScheduler;
import com.hertz.hertz_be.domain.tuningreport.service.WarmupTuningReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningReportVisibilityWriter implements ItemWriter<TuningReport> {

    private final AlarmService alarmService;
    private final TuningReportRepository tuningReportRepository;
    private final TuningReportCacheManager tuningReportCacheManager;
    private final TuningReportFlushScheduler tuningReportFlushScheduler;
    private final WarmupTuningReport warmupTuningReport;

    @Override
    @Transactional
    public void write(Chunk<? extends TuningReport> chunk) {
        String emailDomain = chunk.getItems().getFirst().getEmailDomain();
        int coupleCount = chunk.size();

        for (TuningReport report : chunk.getItems()) {
            report.setVisible();
            tuningReportRepository.save(report);
        }

        registerAfterCommitCallback(() -> {
            try {
                // 알림창 내에서 "새 튜닝 레포트" 알림을 생성하기 위한 메서드
                alarmService.createTuningReportAlarm(emailDomain, coupleCount);
            } catch (Exception e) {
                log.warn("튜닝 레포트 setVisible 배치 작업 이후, 알림 발송 실패: {}", e.getMessage(), e);
            }

            try {
                // 튜닝 레포트를 캐시하고 있는 Redis를 초기화하기 위한 메서드
                setWarmupTuningReport(emailDomain);
            } catch (Exception e) {
                log.warn("튜닝 레포트 setVisible 배치 작업 이후, 워밍업 실패: {}", e.getMessage(), e);
            }
        });
    }

    public void setWarmupTuningReport(String domain) {
        warmupTuningReport.warmupDomain(domain);
    }

    protected void registerAfterCommitCallback(Runnable callback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callback.run();
            }
        });
    }
}
