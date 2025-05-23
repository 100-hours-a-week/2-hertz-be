package com.hertz.hertz_be.domain.channel.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hertz.hertz_be.domain.channel.repository.projection.ChannelRoomProjection;
import com.hertz.hertz_be.global.util.AESUtil;
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

    public static ChannelSummaryDto fromProjectionWithDecrypt(ChannelRoomProjection p, AESUtil aesUtil) {
        String decryptedMessage;
        try {
            decryptedMessage = aesUtil.decrypt(p.getLastMessage());
        } catch (Exception e) {
            decryptedMessage = "메세지를 표시할 수 없습니다.";
        }

        return new ChannelSummaryDto(
                p.getChannelRoomId(),
                p.getPartnerProfileImage(),
                p.getPartnerNickname(),
                decryptedMessage,
                p.getLastMessageTime(),
                p.getIsRead(),
                p.getRelationType()
        );
    }
}
