package com.hertz.hertz_be.domain.channel.repository.projection;

import java.time.LocalDateTime;

public interface ChannelRoomProjection {
    Long getChannelRoomId();
    String getPartnerNickname();
    String getPartnerProfileImage();
    String getLastMessage();
    LocalDateTime getLastMessageTime();
    Boolean getIsRead();
    String getRelationType();
}
