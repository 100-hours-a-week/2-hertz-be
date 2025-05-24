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
    CHAT_ROOM_UPDATE("chat-room-update"),
    NAV_NEW_MESSAGE("nav-new-message"),
    NAV_NO_ANY_NEW_MESSAGE("nav-no-any-new-message");

    private final String value;
}

