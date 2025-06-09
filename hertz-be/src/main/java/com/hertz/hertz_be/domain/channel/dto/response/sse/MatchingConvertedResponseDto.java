package com.hertz.hertz_be.domain.channel.dto.response.sse;

import java.time.LocalDateTime;

public record MatchingConvertedResponseDto(
        Long channelRoomId,
        LocalDateTime matchedAt,
        Long partnerId,
        String partnerNickname
) {}
