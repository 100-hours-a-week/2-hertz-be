//package com.hertz.hertz_be.global.batch;
//
//import jakarta.persistence.EntityManagerFactory;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.batch.core.BatchStatus;
//import org.springframework.batch.core.JobExecution;
//import org.springframework.batch.core.JobParameters;
//import org.springframework.batch.core.JobParametersBuilder;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.batch.test.JobLauncherTestUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
//
//@SpringBootTest
//@EnableBatchProcessing
//@ActiveProfiles("test")
//@DisplayName("튜닝 리포트 배치 테스트")
//class TuningReportBatchTest {
//
//    @Autowired
//    private TuningReportJobLauncher jobLauncher;
//    @Autowired
//    private JobLauncherTestUtils jobLauncherTestUtils;
//
//    @Test
//    @DisplayName("튜닝 리포트 FRIEND 카테고리 배치 실행 테스트")
//    void runFriendCategoryBatch() throws Exception {
//        // given
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("category", "FRIEND")
//                .addLong("timestamp", System.currentTimeMillis())
//                .toJobParameters();
//
//        // when
//        JobExecution execution = jobLauncherTestUtils.launchJob(jobParameters);
//
//        // then
//        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
//    }
//
//    @Test
//    @DisplayName("튜닝 리포트 COUPLE 카테고리 배치 실행 테스트")
//    void runCoupleCategoryBatch() throws Exception {
//        jobLauncher.runGenerationBatch("COUPLE");
//    }
//
//
//    @Test
//    @DisplayName("튜닝 리포트 공개 처리용 배치 실행 테스트")
//    void runTuningReportVisibilityBatch() throws Exception {
//        jobLauncher.runTuningReportVisibilityBatch();
//    }
//
//    @TestConfiguration
//    @EnableTransactionManagement
//    static class BatchTestConfig {
//
//        @Bean
//        public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
//            return new JpaTransactionManager(emf);
//        }
//    }
//}
