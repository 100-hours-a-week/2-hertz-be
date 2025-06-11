package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmMatching;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmMatchingRepository extends JpaRepository<AlarmMatching, Long> {

    List<AlarmMatching> findAllByPartner(User user);

    List<AlarmMatching> findAllBySignalRoomIn(List<SignalRoom> rooms);
}
