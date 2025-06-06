package com.hertz.hertz_be.domain.alarm.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlarmCategory {
    NOTICE("공지 알림", "NOTICE"),
    REPORT("리포트 알림", "REPORT"),
    MATCHING("매칭 결과 알림", "MATCHING");

    private final String label;
    private final String value;
}
