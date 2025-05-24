package com.hertz.hertz_be.domain.channel.dto.response.sse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelListResponseDTO {
    private Long channelRoomId;
    private String partnerProfileImage;
    private String partnerNickname;
    private String lastMessage;
    private String lastMessageTime;
    private boolean isRead;
    private String relationType;

    public Long getChannelRoomId() {
        return channelRoomId;
    }

    public String getPartnerProfileImage() {
        return partnerProfileImage;
    }

    public String getPartnerNickname() {
        return partnerNickname;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastMessageTime() {
        return lastMessageTime;
    }

    public String getRelationType() {
        return relationType;
    }

    @JsonProperty("isRead")
    public boolean getIsRead() {
        return isRead;
    }
}
