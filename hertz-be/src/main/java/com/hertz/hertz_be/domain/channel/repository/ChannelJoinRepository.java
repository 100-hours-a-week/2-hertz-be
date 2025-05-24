package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.ChannelJoin;
import com.hertz.hertz_be.domain.channel.entity.ChannelRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChannelJoinRepository extends JpaRepository<ChannelRoom, Long> {
    Optional<ChannelJoin> findByChannelRoomIdAndUserId(Long channelRoomId, Long userId);

    void delete(ChannelJoin channelJoin);
}
