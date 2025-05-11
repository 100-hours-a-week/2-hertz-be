package com.hertz.hertz_be.domain.channel.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChannelSummaryDto {
    private Long channelRoomId;
    private String partnerProfileImage;
    private String partnerNickname;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    @JsonProperty("isRead")
    private boolean isRead;
    private String relationType;

    public static ChannelSummaryDto fromProjection(ChannelRoomProjection p) {
        return new ChannelSummaryDto(
                p.getChannelRoomId(),
                p.getPartnerProfileImage(),
                p.getPartnerNickname(),
                p.getLastMessage(),
                p.getLastMessageTime(),
                p.getIsRead(),
                p.getRelationType()
        );
    }
}
