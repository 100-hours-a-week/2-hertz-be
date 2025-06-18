package com.hertz.hertz_be.domain.channel.responsecode;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class ChannelNotFoundException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "해당 채널방을 찾을 수 없습니다.";
    private final String code;

    public ChannelNotFoundException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.USER_DEACTIVATED;
    }
}
