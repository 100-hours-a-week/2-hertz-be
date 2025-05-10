package com.hertz.hertz_be.domain.interests.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class InvalidException extends BaseInterestsException {
    private static final String DEFAULT_MESSAGE = "필수 필드 누락 또는 형식 오류가 발생했습니다.";
    private final String code;

    public InvalidException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.REFRESH_TOKEN_INVALID;
    }
}
