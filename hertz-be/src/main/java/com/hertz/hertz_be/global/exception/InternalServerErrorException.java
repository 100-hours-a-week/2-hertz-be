package com.hertz.hertz_be.global.exception;


import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class InternalServerErrorException extends RuntimeException {
    private static final String DEFAULT_MESSAGE = "내부 서버에서 오류가 발생했습니다.";
    private final String code;

    public InternalServerErrorException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.INTERNAL_SERVER_ERROR;
    }

}
