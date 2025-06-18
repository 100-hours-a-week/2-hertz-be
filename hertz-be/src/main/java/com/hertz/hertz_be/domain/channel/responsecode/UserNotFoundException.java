package com.hertz.hertz_be.domain.channel.responsecode;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class UserNotFoundException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "사용자가 존재하지 않습니다.";
    private final String code;

    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.USER_NOT_FOUND;
    }
}
