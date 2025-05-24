package com.hertz.hertz_be.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventName {

    SIGNAL_MATCHING_CONVERSION("signal-matching-conversion"),
    SIGNAL_MATCHING_CONVERSION_IN_ROOM("signal-matching-conversion-in-room"),
    HEARTBEAT("heartbeat"),
    PING("ping"),
    CHAT_ROOM_UPDATE("chat-room-update");

    private final String value;
}

