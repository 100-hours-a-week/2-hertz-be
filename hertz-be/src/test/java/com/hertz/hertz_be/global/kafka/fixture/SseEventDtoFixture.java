package com.hertz.hertz_be.global.kafka.fixture;


import com.hertz.hertz_be.global.kafka.dto.SseEventDto;

public class SseEventDtoFixture {

    public static final Long DEFAULT_USER_ID = 123L;
    public static final String DEFAULT_EVENT_NAME = "event";
    public static final String DEFAULT_DATA = "data";

    public static SseEventDto defaultEvent() {
        return new SseEventDto(DEFAULT_USER_ID, DEFAULT_EVENT_NAME, DEFAULT_DATA);
    }

    public static SseEventDto withAll(Long userId, String eventName, String data) {
        return new SseEventDto(userId, eventName, data);
    }
}
