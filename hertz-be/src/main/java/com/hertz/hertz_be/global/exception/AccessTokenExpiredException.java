package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import org.springframework.security.core.AuthenticationException;
import lombok.Getter;

@Getter
public class AccessTokenExpiredException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "Access Token이 만료되었습니다. Refresh Token으로 재발급 요청이 필요합니다.";
    private final String code;

    public AccessTokenExpiredException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.ACCESS_TOKEN_EXPIRED;
    }

}
