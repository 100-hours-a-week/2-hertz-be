package com.hertz.hertz_be.global.kafka.servise;

import com.hertz.hertz_be.global.kafka.exception.KafkaException;
import com.hertz.hertz_be.global.sse.SseService;
import com.hertz.hertz_be.global.kafka.dto.SseEventDto;
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
    public void consumeToSse(SseEventDto event, Acknowledgment ack) {
        try {
            if(sseService.sendToClient(event.userId(), event.eventName(), event.data())) {
                log.info("✅ Kafka → SSE 전송 성공: userId= {}, event-name= {}", event.userId(), event.eventName());
                ack.acknowledge();
            }
        } catch (Exception e) {
            throw new KafkaException(String.format(
                    "Kafka → SSE 처리 중 알 수 없는 예외 발생: userId=%d, event=%s, 재시도 실행",
                    event.userId(), event.eventName()), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topic.sse.dlq.name}",
            groupId = "${kafka.consumer.sse.dlq.group-id}"
    )
    public void consumeDlq(SseEventDto failedEvent) {
        log.error("🔥 Kafka SSE DLQ에 저장된 실패 이벤트: userId={}, event={}", failedEvent.userId(), failedEvent.eventName());
    }
}
