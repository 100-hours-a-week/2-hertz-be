package com.hertz.hertz_be.global.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TuningReportJobLauncher {

    @Qualifier("tuningReportGenerationJob")
    private final Job tuningReportGenerationJob;

    @Qualifier("tuningReportVisibilityJob")
    private final Job tuningReportVisibilityJob;

    private final JobLauncher jobLauncher;

    public void runGenerationBatch(String category) throws Exception {
        Long timestamp = java.time.LocalDateTime.now()
                .minusDays(1)
                .withHour(23).withMinute(59).withSecond(59)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("category", category)
                .addLong("timestamp", timestamp)
                .toJobParameters();

        jobLauncher.run(tuningReportGenerationJob, jobParameters);
    }

    public void runTuningReportVisibilityBatch() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis()) // JobInstance를 구분하기 위한 구분자 역할, 파라미터가 없어도 넣음
                .toJobParameters();

        jobLauncher.run(tuningReportVisibilityJob, jobParameters);
    }
}
