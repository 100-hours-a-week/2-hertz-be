package com.hertz.hertz_be.domain.channel.dto.socketio;

public record MessageRequest(
        Long roomId,
        String message
) {}