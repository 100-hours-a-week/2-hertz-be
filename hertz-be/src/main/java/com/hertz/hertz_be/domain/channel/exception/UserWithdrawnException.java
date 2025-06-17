package com.hertz.hertz_be.domain.channel.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class UserWithdrawnException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "상대방이 탈퇴한 사용자입니다.";
    private final String code;

    public UserWithdrawnException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.USER_DEACTIVATED;
    }
}
