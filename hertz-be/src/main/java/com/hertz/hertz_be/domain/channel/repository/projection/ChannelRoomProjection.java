package com.hertz.hertz_be.domain.channel.repository.projection;

import java.time.LocalDateTime;

public interface ChannelRoomProjection {
    Long getChannelRoomId();
    Boolean getIsRead();
    String getLastMessage();
    LocalDateTime getLastMessageTime();
    String getRelationType();
    String getPartnerNickname();
    String getPartnerProfileImage();
    LocalDateTime getSenderExitedAt();
    LocalDateTime getReceiverExitedAt();
    Long getSenderUserId();
    Long getReceiverUserId();
}
