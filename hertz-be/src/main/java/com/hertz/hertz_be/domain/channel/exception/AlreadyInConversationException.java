package com.hertz.hertz_be.domain.channel.exception;

import com.hertz.hertz_be.global.common.ResponseCode;

public class AlreadyInConversationException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "이미 대화 중인 상대방입니다.";
    private final String code;

    public AlreadyInConversationException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.ALREADY_IN_CONVERSATION;
    }

    public String getCode() {
        return code;
    }
}
