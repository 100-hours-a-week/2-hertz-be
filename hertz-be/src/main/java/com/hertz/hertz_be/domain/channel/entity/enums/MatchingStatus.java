package com.hertz.hertz_be.domain.channel.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchingStatus {
    SIGNAL("시그널", "SIGNAL"),
    MATCHED("매칭 수락", "MATCHING"),
    UNMATCHED("매칭 거절", "UNMATCHED");

    private final String label;
    private final String value;
}
