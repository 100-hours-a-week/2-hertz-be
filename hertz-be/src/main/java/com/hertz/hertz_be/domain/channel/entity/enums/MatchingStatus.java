package com.hertz.hertz_be.domain.channel.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingStatus {
    SIGNAL("시그널"),
    MATCHED("매칭 수락"),
    UNMATCHED("매칭 거절");

    private final String label;
}
