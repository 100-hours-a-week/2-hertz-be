package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmAlert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmAlertRepository extends JpaRepository<AlarmAlert, Long> {
}
