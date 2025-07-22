package com.hertz.hertz_be.global.kafka.dto;

public record SseEventDto(
        Long userId,
        String eventName,
        Object data
) {}
