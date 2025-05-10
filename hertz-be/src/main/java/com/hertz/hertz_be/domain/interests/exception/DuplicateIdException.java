package com.hertz.hertz_be.domain.interests.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class DuplicateIdException extends BaseInterestsException {
    private static final String DEFAULT_MESSAGE = "중복된 사용자 아이디 입니다.";
    private final String code;

    public DuplicateIdException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.REFRESH_TOKEN_INVALID;
    }
}
