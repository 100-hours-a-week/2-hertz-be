package com.hertz.hertz_be.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TuningReportScheduler {

    private final Job tuningReportJob;
    private final JobLauncher jobLauncher;

    /**
     * 매주 월요일 오전 6시에 실행
     */
    @Scheduled(cron = "0 0 6 * * MON,WED") // 월요일 & 수요일 오전 6시
    public void runCategoryBasedTuningReport() {
        String category =
            switch (LocalDate.now().getDayOfWeek()) {
                case MONDAY -> "LOVER";
                case WEDNESDAY -> "FRIEND";
                default -> throw new IllegalStateException("지원하지 않는 요일");
            };

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("category", category)
                    .toJobParameters();

            jobLauncher.run(tuningReportJob, params);
        } catch (Exception e) {
            log.error("💥 튜닝 리포트 실행 실패", e);
        }
    }
}
