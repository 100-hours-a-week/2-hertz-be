package com.hertz.hertz_be.global.kafka.dto;

public record SseEvent(
        Long userId,
        String eventName,
        Object data
) {}
