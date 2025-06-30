package com.hertz.hertz_be.global.socketio.dto;

import java.time.LocalDateTime;

public record SocketIoMessageRequest(
        Long roomId,
        String message,
        LocalDateTime sendAt
) {}