package com.hertz.hertz_be.global.kafka.servise;


import com.hertz.hertz_be.global.sse.SseService;
import com.hertz.hertz_be.global.kafka.dto.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final SseService sseService;

    @KafkaListener(
            topics = "healthcheck-topic",
            groupId = "${spring.kafka.consumer.healthcheck-topic.group-id}",
            containerFactory = "stringKafkaListener"
    )
    public void consumeHealthcheck(String message) {
        log.info("Kafka 메시지 수신: {}", message);
    }

    @KafkaListener(
            topics = "${kafka.topic.sse.name}",
            groupId = "${kafka.sse.consumer.sse-group-id}",
            containerFactory = "sseKafkaListener"
    )
    public void consumeToSse(SseEvent event, Acknowledgment ack) {
        boolean sent = sseService.sendToClient(event.userId(), event.eventName(), event.data());
        if (sent) {
            ack.acknowledge();
            log.info("Kafka → SSE: userId={}, event={}", event.userId(), event.eventName());
        }
    }
}
