package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record MatchingConvertedInChannelRoomResponseDto(
        Long channelRoomId,
        String partnerNickname,
        boolean hasResponded,
        boolean partnerHasResponded
) {}
