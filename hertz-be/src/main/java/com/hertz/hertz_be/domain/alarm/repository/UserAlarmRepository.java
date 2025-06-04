package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.UserAlarm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {
}
