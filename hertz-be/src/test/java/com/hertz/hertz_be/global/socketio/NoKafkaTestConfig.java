package com.hertz.hertz_be.global.socketio;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class NoKafkaTestConfig {
    @Bean(name = "kafkaListenerContainerFactory")
    public org.springframework.kafka.config.KafkaListenerContainerFactory<?> kafkaListenerContainerFactory() {
        return org.mockito.Mockito.mock(org.springframework.kafka.config.KafkaListenerContainerFactory.class);
    }
}

