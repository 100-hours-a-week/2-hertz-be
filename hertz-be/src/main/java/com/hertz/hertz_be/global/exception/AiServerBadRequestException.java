package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class AiServerBadRequestException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "AI 서버로 올바르지 않은 요청이 전달되었습니다.";
    private final String code;

    public AiServerBadRequestException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.TUNING_BAD_REQUEST;
    }

}
