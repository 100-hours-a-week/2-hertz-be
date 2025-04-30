package com.hertz.hertz_be.auth.exception;

import com.hertz.hertz_be.global.common.ResponseCode;

public class RefreshTokenInvalidException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Refresh Token이 유효하지 않거나 만료되었습니다. 다시 로그인 해주세요.";
    private final String code;

    public RefreshTokenInvalidException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.REFRESH_TOKEN_INVALID;
    }

    public String getCode() {
        return code;
    }
}