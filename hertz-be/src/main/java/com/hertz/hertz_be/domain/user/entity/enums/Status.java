package com.hertz.hertz_be.domain.user.entity.enums;

public enum Status {

    ACTIVE("활성화"),
    INACTIVE("비활성화"),
    DELETED("탈퇴");
    private final String label;

    Status(String label) {
        this.label = label;
    }

}
