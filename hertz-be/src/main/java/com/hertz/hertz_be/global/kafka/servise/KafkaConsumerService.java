package com.hertz.hertz_be.global.kafka.servise;


import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class KafkaConsumerService {

    @KafkaListener(topics = "test-topic", groupId = "hertz-group")
    public void listen(String message) {
        log.info("Kafka 메시지 수신: {}", message);
    }
}
