package com.hertz.hertz_be.domain.interests.entity.enums;

import lombok.Getter;

@Getter
public enum InterestsCategoryType {
    KEYWORD("키워드"),
    INTEREST("관심사");

    private final String label;

    InterestsCategoryType(String label) {
        this.label = label;
    }
}
