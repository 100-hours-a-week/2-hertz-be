package com.hertz.hertz_be.domain.channel.repository.projection;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SignalRoomRepositoryCustom {
    List<Long> findSignalRoomIdsOrderByLastMessageTime(Long userId, int offset, int limit);
    Page<SignalRoom> findAllOrderByLastMessageTimeWithUsers(Long userId, Pageable pageable);
}

