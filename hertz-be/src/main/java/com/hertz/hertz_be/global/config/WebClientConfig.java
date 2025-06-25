package com.hertz.hertz_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.server.ip}")
    private String aiServerIp;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient tuningAiServerWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(aiServerIp)
                .build();
    }
}
