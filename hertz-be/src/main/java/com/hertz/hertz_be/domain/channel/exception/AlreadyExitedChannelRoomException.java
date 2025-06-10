package com.hertz.hertz_be.domain.channel.exception;

import com.hertz.hertz_be.global.common.ResponseCode;
import lombok.Getter;

@Getter
public class AlreadyExitedChannelRoomException extends BaseChannelException {
  private static final String DEFAULT_MESSAGE = "이미 나간 채팅방입니다.";
  private final String code;

  public AlreadyExitedChannelRoomException() {
    super(DEFAULT_MESSAGE);
    this.code = ResponseCode.ALREADY_EXITED_CHANNEL_ROOM;
  }
}
