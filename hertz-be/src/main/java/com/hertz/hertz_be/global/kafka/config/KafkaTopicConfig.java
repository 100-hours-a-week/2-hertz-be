package com.hertz.hertz_be.global.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.bootstrapAddress}")
    private String bootstrapAddress;

    @Value("${kafka.topic.see.numPartitions}")
    private String topicName;

    @Value("${kafka.topic.see.numPartitions}")
    private int numPartitions;

    @Value("${kafka.topic.sse.replicationFactor}")
    private short replicationFactor;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic sseTopic() {
        return new NewTopic(topicName, numPartitions, replicationFactor);
    }

    @Bean
    public NewTopic healthCheckTopic() {
        return TopicBuilder.name("healthcheck-topic")
                .partitions(1)
                .replicas(3)
                .build();
    }
}
