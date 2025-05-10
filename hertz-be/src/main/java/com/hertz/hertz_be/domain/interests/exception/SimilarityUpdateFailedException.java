package com.hertz.hertz_be.domain.interests.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class SimilarityUpdateFailedException extends BaseInterestsException {

    private static final String DEFAULT_MESSAGE = "AI 처리 과정에서 오류가 발생했습니다.";
    private final String code;

    public SimilarityUpdateFailedException() {
        super(DEFAULT_MESSAGE);
        this.code = ResponseCode.REFRESH_TOKEN_INVALID;
    }

}