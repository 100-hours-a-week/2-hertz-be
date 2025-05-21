package com.hertz.hertz_be.global.config;

import org.springframework.retry.support.RetryTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(1000)
                .retryOn(Exception.class)
                .build();
    }
}
