package com.hertz.hertz_be.domain.channel.repository;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SignalRoomRepository extends JpaRepository<SignalRoom, Long> {
    boolean existsBySenderUserAndReceiverUser(User sender, User receiver);
    Optional<SignalRoom> findByUserPairSignal(String userPairSignal);
}
