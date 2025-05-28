package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewMessageResponseDto {
    private Long channelRoomId;
    private Long partnerId;
    private String partnerNickname;
    private String message;
}
