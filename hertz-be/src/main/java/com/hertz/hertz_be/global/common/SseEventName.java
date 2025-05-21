package com.hertz.hertz_be.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SseEventName {

    SIGNAL_MATCHING_CONVERSION("signal-matching-conversion"),
    SIGNAL_MATCHING_CONVERSION_IN_ROOM("signal-matching-conversion-in-room"),
    PING("ping");

    private final String value;
}

