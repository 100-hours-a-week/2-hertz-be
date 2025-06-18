package com.hertz.hertz_be.domain.channel.responsecode;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class InterestsNotSelectedException extends BaseChannelException{
    private static final String DEFAULT_MESSAGE = "사용자가 아직 취향 선택을 완료하지 않았습니다.";
    private final String code;

    public InterestsNotSelectedException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.USER_INTERESTS_NOT_SELECTED;
    }

}
