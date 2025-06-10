//package com.hertz.hertz_be.global.batch;
//
//import jakarta.persistence.EntityManagerFactory;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//import org.springframework.orm.jpa.JpaTransactionManager;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.annotation.EnableTransactionManagement;
//
//@SpringBootTest
//@EnableBatchProcessing
//@ActiveProfiles("test")
//@DisplayName("튜닝 리포트 배치 테스트")
//class TuningReportBatchTest {
//
//    @Autowired
//    private TuningReportJobLauncher jobLauncher;
//
//    @Test
//    @DisplayName("COUPLE 카테고리 배치 실행 테스트")
//    void runLoverCategoryBatch() throws Exception {
//        jobLauncher.runBatch("COUPLE");
//    }
//
//    @Test
//    @DisplayName("FRIEND 카테고리 배치 실행 테스트")
//    void runFriendCategoryBatch() throws Exception {
//        jobLauncher.runBatch("FRIEND");
//    }
//
//    @Test
//    @DisplayName("MEAL_FRIEND 카테고리 배치 실행 테스트")
//    void runMealFriendCategoryBatch() throws Exception {
//        jobLauncher.runBatch("MEAL_FRIEND");
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
