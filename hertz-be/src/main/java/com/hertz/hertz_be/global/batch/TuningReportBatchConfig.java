package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.tuningreport.entity.TuningReport;
import com.hertz.hertz_be.global.exception.AiServerBadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class TuningReportBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TuningReportReader tuningReportReader;
    private final TuningReportWriter tuningReportWriter;
    private final TuningReportProcessor tuningReportProcessor;

    private static final int CHUNK_SIZE = 20;

    @Bean
    public Job tuningReportGenerationJob(Step tuningReportStep) {
        return new JobBuilder("TuningReportGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tuningReportStep)
                .build();
    }

    @Bean
    public Step tuningReportGenerationStep(JpaPagingItemReader<SignalRoom> reader) {
        return new StepBuilder("TuningReportGenerationStep", jobRepository)
                .<SignalRoom, TuningReport>chunk(CHUNK_SIZE, transactionManager)
                .reader(reader)
                .processor(tuningReportProcessor)
                .writer(tuningReportWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(AiServerBadRequestException.class)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<SignalRoom> reader(
            @Value("#{jobParameters['category']}") String category) {
        return tuningReportReader.reader(category);
    }
}