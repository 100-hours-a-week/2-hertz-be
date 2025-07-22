package com.hertz.hertz_be.global.socketio.dto;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;

import java.time.format.DateTimeFormatter;

public record SocketIoMessageResponse(
        Long roomId,
        Long senderId,
        String message,
        String sendAt,
        Long messageId
){
    public static SocketIoMessageResponse from (SignalMessage message) {
        return new SocketIoMessageResponse(
                message.getSignalRoom().getId(),
                message.getSenderUser().getId(),
                message.getMessage(),
                message.getSendAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                message.getId()
        );
    }

    public static SocketIoMessageResponse from (SignalMessage message, String decryptMessage) {
        return new SocketIoMessageResponse(
                message.getSignalRoom().getId(),
                message.getSenderUser().getId(),
                decryptMessage,
                message.getSendAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                message.getId()
        );
    }
}
