package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmNotification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmNotificationRepository extends JpaRepository<AlarmNotification, Long> {
}
