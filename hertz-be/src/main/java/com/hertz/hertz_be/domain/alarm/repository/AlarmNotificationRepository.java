package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmNotification;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmNotificationRepository extends JpaRepository<AlarmNotification, Long> {
    List<AlarmNotification> findAllByWriter(User user);
}
