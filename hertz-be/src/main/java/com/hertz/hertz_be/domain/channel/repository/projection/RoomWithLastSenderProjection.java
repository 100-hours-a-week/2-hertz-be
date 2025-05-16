package com.hertz.hertz_be.domain.channel.repository.projection;

public interface RoomWithLastSenderProjection {
    Long getLastSenderId();           // 마지막 메시지 보낸 유저 ID
}