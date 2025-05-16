package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class AiServerNotFoundException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "AI 서버에 해당 사용자 정보가 없습니다.";
    private final String code;

    public AiServerNotFoundException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.TUNING_NOT_FOUND_USER;
    }

}

