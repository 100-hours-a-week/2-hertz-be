package com.hertz.hertz_be.global;


public class TestLoginRequestDTO {

    private Long userId; // 간단히 userId만 받는다고 가정

    public TestLoginRequestDTO() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}