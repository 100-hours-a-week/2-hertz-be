package com.hertz.hertz_be.domain.channel.dto.response.sse;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifyNewMessageResponseDTO {
    private Long channelRoomId;
    private Long partnerId;
    private String partnerNickname;
    private String message;
}
