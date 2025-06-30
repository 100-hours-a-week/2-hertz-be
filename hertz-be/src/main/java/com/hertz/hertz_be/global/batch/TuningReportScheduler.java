package com.hertz.hertz_be.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class TuningReportScheduler {

    @Qualifier("tuningReportGenerationJob")
    private final Job tuningReportGenerationJob;

    @Qualifier("tuningReportVisibilityJob")
    private final Job tuningReportVisibilityJob;

    private final JobLauncher jobLauncher;

    /**
     * 매주 월요일 & 수요일 오전 6시에 튜닝 리포트 생성
     */
    //@Scheduled(cron = "0 0 6 * * MON,WED")
    @Scheduled(cron = "0 */3 * * * *")
    public void runCategoryBasedTuningReport() {
        String category = "FRIEND";
//        String category =
//                switch (LocalDate.now().getDayOfWeek()) {
//                    case MONDAY -> "LOVER";
//                    case WEDNESDAY -> "FRIEND";
//                    default -> throw new IllegalStateException("지원하지 않는 요일");
//                };

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("category", category)
                    .toJobParameters();

            jobLauncher.run(tuningReportGenerationJob, params);
        } catch (Exception e) {
            log.error("💥 튜닝 리포트 실행 실패", e);
        }
    }

    /**
     * 매일 오후 12:30에 튜닝 리포트 공개 처리 실행
     */
    @Scheduled(cron = "0 30 12 * * MON,WED")
    public void runTuningReportVisibilityUpdate() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(tuningReportVisibilityJob, params);
        } catch (Exception e) {
            log.error("💥 튜닝 리포트 공개 처리 실패", e);
        }
    }
}
