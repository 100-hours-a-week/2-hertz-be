package com.hertz.hertz_be.global.socketio.dto;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;

import java.time.LocalDateTime;

public record SocketIoMessageResponse(
        Long roomId,
        Long senderId,
        String message,
        LocalDateTime sendAt,
        Long messageId
){
    public static SocketIoMessageResponse from (SignalMessage message) {
        return new SocketIoMessageResponse(
                message.getSignalRoom().getId(),
                message.getSenderUser().getId(),
                message.getMessage(),
                message.getSendAt(),
                message.getId()
        );
    }

    public static SocketIoMessageResponse from (SignalMessage message, String decryptMessage) {
        return new SocketIoMessageResponse(
                message.getSignalRoom().getId(),
                message.getSenderUser().getId(),
                decryptMessage,
                message.getSendAt(),
                message.getId()
        );
    }
}
