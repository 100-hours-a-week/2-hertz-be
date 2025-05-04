package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SignalMessageRepository extends JpaRepository<SignalMessage, Long> {
    boolean existsBySignalRoomIdInAndSenderUserIdNotAndIsReadFalse(List<SignalRoom> signalRooms, User senderUser);

}
