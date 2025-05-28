package com.hertz.hertz_be.domain.auth.dto.request;


public class TestLoginRequestDto {

    private Long userId; // 간단히 userId만 받는다고 가정

    public TestLoginRequestDto() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
