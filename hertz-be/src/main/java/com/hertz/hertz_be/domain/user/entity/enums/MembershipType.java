package com.hertz.hertz_be.domain.user.entity.enums;

public enum MembershipType {
    GENERAL_USER("일반 사용자"),
    PREMIUM_USER("멤버십 사용자");

    private final String label;

    MembershipType(String label) {this.label = label;}
}
