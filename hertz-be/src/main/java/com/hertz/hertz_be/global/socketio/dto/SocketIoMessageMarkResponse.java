package com.hertz.hertz_be.global.socketio.dto;


import java.time.LocalDateTime;

public record SocketIoMessageMarkResponse(
        Long roomId,
        Long userId,
        LocalDateTime readAt
) {
    public static SocketIoMessageMarkResponse from(Long roomId, Long userId) {
        return new SocketIoMessageMarkResponse(roomId, userId, LocalDateTime.now());
    }
}