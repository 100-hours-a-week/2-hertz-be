package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.global.exception.AiServerBadRequestException;
import com.hertz.hertz_be.global.infra.ai.dto.request.AiTuningReportGenerationRequest;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@EnableBatchProcessing
public class TuningReportGenerationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final TuningReportGenerationReader tuningReportGenerationReader;
    private final TuningReportGenerationCustomReader tuningReportGenerationCustomReader; // 커스텀 reader 추가
    private final TuningReportGenerationWriter tuningReportGenerationWriter;
    private final TuningReportGenerationProcessor tuningReportGenerationProcessor;
    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job tuningReportGenerationJob() {
        return new JobBuilder("TuningReportGenerationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tuningReportGenerationStep(null, null))
                .build();
    }

    @Bean
    @StepScope
    public TuningReportGenerationCustomReader tuningReportGenerationCustomReader(
            @Value("#{jobParameters['category']}") String category,
            @Value("#{jobParameters['timestamp']}") Long timestamp
    ) {
        return new TuningReportGenerationCustomReader(
                signalRoomRepository,
                signalMessageRepository,
                category,
                timestamp
        );
    }

    @Bean
    @StepScope
    public Step tuningReportGenerationStep(
            @Value("#{jobParameters['category']}") String category,
            @Value("#{jobParameters['timestamp']}") Long timestamp
    ) {
        return new StepBuilder("TuningReportGenerationStep", jobRepository)
                .<SignalRoom, AiTuningReportGenerationRequest>chunk(CHUNK_SIZE, transactionManager)
                //.reader(tuningReportGenerationReader.reader(category, timestamp))
                .reader(tuningReportGenerationCustomReader)
                .processor(tuningReportGenerationProcessor)
                .writer(tuningReportGenerationWriter)
                .faultTolerant()
                .retryLimit(3)
                .retry(AiServerBadRequestException.class)
                .build();
    }

    @Bean
    public Job tuningReportGenerationJobForFriend() {
        return new JobBuilder("TuningReportGenerationJobForFriend", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tuningReportGenerationStepForFriend())
                .build();
    }

    @Bean
    public Step tuningReportGenerationStepForFriend() {
        return new StepBuilder("TuningReportGenerationStepFor", jobRepository)
                .<SignalRoom, AiTuningReportGenerationRequest>chunk(CHUNK_SIZE, transactionManager)
                .reader(tuningReportGenerationReader.reader("FRIEND", System.currentTimeMillis()))
                .processor(tuningReportGenerationProcessor)
                .writer(tuningReportGenerationWriter)
                .build();
    }

    @Bean
    public Job tuningReportGenerationJobForCouple() {
        return new JobBuilder("tuningReportGenerationJobForCouple", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(tuningReportGenerationStepForCoupled())
                .build();
    }

    @Bean
    public Step tuningReportGenerationStepForCoupled() {
        return new StepBuilder("TuningReportGenerationStepFor", jobRepository)
                .<SignalRoom, AiTuningReportGenerationRequest>chunk(CHUNK_SIZE, transactionManager)
                .reader(tuningReportGenerationReader.reader("COUPLE", System.currentTimeMillis()))
                .processor(tuningReportGenerationProcessor)
                .writer(tuningReportGenerationWriter)
                .build();
    }

}
