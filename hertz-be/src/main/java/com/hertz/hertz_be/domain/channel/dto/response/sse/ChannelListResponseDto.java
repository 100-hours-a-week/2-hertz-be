package com.hertz.hertz_be.domain.channel.dto.response.sse;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelListResponseDto(
        Long channelRoomId,
        String partnerProfileImage,
        String partnerNickname,
        String lastMessage,
        String lastMessageTime,
        @JsonProperty("isRead") boolean isRead,
        String relationType
) {}
