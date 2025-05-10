package com.hertz.hertz_be.domain.interests.exception;

import lombok.Getter;

@Getter
public class RegisterBadRequestException extends BaseInterestsException {

    private static final String DEFAULT_MESSAGE = "AI 서버로 잘못된 요청을 보냈습니다.";
    private final String code;

    public RegisterBadRequestException(String code) {
        super(DEFAULT_MESSAGE);
        this.code = code;
    }

}