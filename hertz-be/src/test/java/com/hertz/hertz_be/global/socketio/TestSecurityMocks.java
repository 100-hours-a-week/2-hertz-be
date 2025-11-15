package com.hertz.hertz_be.global.socketio;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestSecurityMocks {
    @Bean
    public com.hertz.hertz_be.global.auth.filter.JwtAuthenticationFilter jwtAuthenticationFilter() {
        return org.mockito.Mockito.mock(com.hertz.hertz_be.global.auth.filter.JwtAuthenticationFilter.class);
    }

    @Bean
    public com.hertz.hertz_be.global.auth.filter.SseAuthenticationFilter sseAuthenticationFilter() {
        return org.mockito.Mockito.mock(com.hertz.hertz_be.global.auth.filter.SseAuthenticationFilter.class);
    }

    @Bean
    public com.hertz.hertz_be.global.auth.handler.CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
        return org.mockito.Mockito.mock(com.hertz.hertz_be.global.auth.handler.CustomAuthenticationEntryPoint.class);
    }
}

