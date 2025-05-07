package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class AiServerErrorException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "AI 서버 문제로 요청을 처리할 수 없습니다.";
    private final String code;

    public AiServerErrorException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.AI_SERVER_ERROR;
    }

}

