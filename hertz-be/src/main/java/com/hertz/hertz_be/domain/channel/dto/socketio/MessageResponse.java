package com.hertz.hertz_be.domain.channel.dto.socketio;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;

import java.time.LocalDateTime;

public record MessageResponse (
        Long roomId,
        Long senderId,
        String message,
        LocalDateTime sendAt
){
    public static MessageResponse from (SignalMessage message) {
        return new MessageResponse(
                message.getSignalRoom().getId(),
                message.getSenderUser().getId(),
                message.getMessage(),
                message.getSendAt()
        );
    }
}
