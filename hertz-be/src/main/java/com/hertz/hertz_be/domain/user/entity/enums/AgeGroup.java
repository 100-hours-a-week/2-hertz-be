package com.hertz.hertz_be.domain.user.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgeGroup {

    AGE_20S("20대"),
    AGE_30S("30대"),
    AGE_40S("40대"),
    AGE_50S("50대"),
    AGE_60_PLUS("60대 이상");

    private final String label;
}