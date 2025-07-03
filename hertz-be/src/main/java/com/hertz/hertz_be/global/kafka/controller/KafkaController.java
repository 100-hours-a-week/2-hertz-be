package com.hertz.hertz_be.global.kafka.controller;


import com.hertz.hertz_be.global.kafka.servise.KafkaProducerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/kafka")
@RequiredArgsConstructor
@Tag(name = "Kafka 관련 API")
public class KafkaController {

    private final KafkaProducerService producerService;

    @PostMapping("/ping")
    @Operation(summary = "Kafka 헬스체크를 위한 API")
    public String sendMessage(@RequestParam String message) {
        producerService.sendMessage("healthcheck-topic", message);
        return "pong";
    }
}
