package com.hertz.hertz_be.domain.channel.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class UnauthorizedAccessException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "해당 채널방에 접근 권한이 없습니다.";
    private final String code;

    public UnauthorizedAccessException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.ALREADY_IN_CONVERSATION;
    }

}