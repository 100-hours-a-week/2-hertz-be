package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MatchingConvertedInChannelRoomResponseDTO {
    private Long channelRoomId;
    private boolean hasResponded;
}
