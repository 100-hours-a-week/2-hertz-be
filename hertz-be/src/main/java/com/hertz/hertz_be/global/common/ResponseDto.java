package com.hertz.hertz_be.global.common;

import lombok.Getter;

@Getter
public class ResponseDto<T> {
    private final String code;
    private final String message;
    private final T data;

    public ResponseDto(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
}
