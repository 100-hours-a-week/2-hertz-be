package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchingConvertedInChannelRoomResponseDto {
    private Long channelRoomId;
    private boolean hasResponded;
}
