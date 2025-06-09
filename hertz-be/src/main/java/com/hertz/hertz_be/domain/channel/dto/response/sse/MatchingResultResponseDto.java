package com.hertz.hertz_be.domain.channel.dto.response.sse;

public record MatchingResultResponseDto(
        Long channelRoomId,
        Long partnerId,
        String partnerProfileImage,
        String partnerNickname
) {}
