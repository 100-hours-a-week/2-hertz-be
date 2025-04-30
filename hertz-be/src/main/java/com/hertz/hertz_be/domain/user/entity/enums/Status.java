package com.hertz.hertz_be.domain.user.entity.enums;

public enum Status {
    GENERAL_USER("일반 사용자"),
    MEMBERSHIP_USER("멤버십 사용자");

    private final String label;

    Status(String label) {this.label = label;}
}
