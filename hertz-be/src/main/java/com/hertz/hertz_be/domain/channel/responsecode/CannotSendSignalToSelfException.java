package com.hertz.hertz_be.domain.channel.responsecode;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class CannotSendSignalToSelfException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "자기 자신에게는 시그널을 보낼 수 없습니다.";
    private final String code;

    public CannotSendSignalToSelfException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.ALREADY_IN_CONVERSATION;
    }

}
