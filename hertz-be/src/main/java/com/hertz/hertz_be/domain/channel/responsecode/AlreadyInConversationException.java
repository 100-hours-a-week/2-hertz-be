package com.hertz.hertz_be.domain.channel.responsecode;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class AlreadyInConversationException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "이미 대화 중인 상대방입니다.";
    private final String code;

    public AlreadyInConversationException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.ALREADY_IN_CONVERSATION;
    }

}
