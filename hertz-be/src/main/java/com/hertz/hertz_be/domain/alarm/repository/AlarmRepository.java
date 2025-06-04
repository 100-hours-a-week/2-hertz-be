package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.Alarm;
import lombok.Lombok;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
}
