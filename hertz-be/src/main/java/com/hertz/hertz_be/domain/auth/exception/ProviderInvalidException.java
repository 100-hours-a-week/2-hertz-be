package com.hertz.hertz_be.domain.auth.exception;

import com.hertz.hertz_be.global.common.ResponseCode;

public class ProviderInvalidException extends BaseAuthException{
    private static final String DEFAULT_MESSAGE = "지원하지 않는 OAuth provider입니다.";
    private final String code;

    public ProviderInvalidException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.UNSUPPORTED_PROVIDER;
    }

    public String getCode() {
        return code;
    }
}
