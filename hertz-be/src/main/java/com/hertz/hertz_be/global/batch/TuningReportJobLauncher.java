package com.hertz.hertz_be.global.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TuningReportJobLauncher {

    private final Job tuningReportJob;
    private final JobLauncher jobLauncher;

    public void runBatch(String category) throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("category", category)
                .addLong("timestamp", System.currentTimeMillis()) // 매번 다른 JobInstance로 실행
                .toJobParameters();

        jobLauncher.run(tuningReportJob, jobParameters);
    }
}
