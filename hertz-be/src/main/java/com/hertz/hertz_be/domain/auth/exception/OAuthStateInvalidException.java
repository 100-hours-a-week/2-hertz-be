package com.hertz.hertz_be.domain.auth.exception;

import com.hertz.hertz_be.global.common.ResponseCode;

import lombok.Getter;

@Getter
public class OAuthStateInvalidException extends BaseAuthException{
    private static final String DEFAULT_MESSAGE = "잘못된 인증 요청입니다. 다시 로그인해주세요.";
    private final String code;

    public OAuthStateInvalidException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.OAUTH_STATE_INVALID;
    }
}
