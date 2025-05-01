package com.hertz.hertz_be.domain.auth.exception;

import com.hertz.hertz_be.global.common.ResponseCode;

public class RateLimitException extends BaseAuthException{
    private static final String DEFAULT_MESSAGE = "로그인 요청 제한(Rate Limit)에 걸렸습니다. 잠시 후 다시 시도해주세요.";
    private final String code;

    public RateLimitException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.RATE_LIMIT;
    }

    public String getCode() {
        return code;
    }
}
