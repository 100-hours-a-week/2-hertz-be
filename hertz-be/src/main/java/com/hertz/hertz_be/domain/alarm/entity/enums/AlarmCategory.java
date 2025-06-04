package com.hertz.hertz_be.domain.alarm.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AlarmCategory {
    NOTICE("공지"),
    REPORT("튜닝 리포트"),
    MATCHING("매칭 결과");

    private final String label;
}
