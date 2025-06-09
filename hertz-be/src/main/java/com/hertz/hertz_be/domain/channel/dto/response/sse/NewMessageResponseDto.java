package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.*;

public record NewMessageResponseDto(
        Long channelRoomId,
        Long partnerId,
        String partnerNickname,
        String message,
        String messageSendAt,
        String partnerProfileImage,
        String relationType
) {}
