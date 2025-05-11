package com.hertz.hertz_be.domain.interests.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class InvalidInterestsInputException extends BaseInterestsException {

  private static final String DEFAULT_MESSAGE = "하나 이상의 항목을 선택해야 합니다.";
  private final String code;

  public InvalidInterestsInputException() {
    super(DEFAULT_MESSAGE);
    this.code = ResponseCode.EMPTY_LIST_NOT_ALLOWED;
  }

}