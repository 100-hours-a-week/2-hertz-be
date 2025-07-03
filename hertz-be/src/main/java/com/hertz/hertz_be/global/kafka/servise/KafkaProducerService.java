package com.hertz.hertz_be.global.kafka.servise;

import com.hertz.hertz_be.global.kafka.dto.SseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    @Value("${kafka.topic.sse.name}")
    private String SseEventTopicName;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTemplate<String, SseEvent> kafkaTemplateForSee;

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    public void send(SseEvent event) {
        kafkaTemplateForSee.send(SseEventTopicName, event.userId().toString(), event);
    }
}
