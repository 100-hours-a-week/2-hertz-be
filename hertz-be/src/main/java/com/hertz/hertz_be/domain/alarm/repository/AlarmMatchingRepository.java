package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmMatching;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmMatchingRepository extends JpaRepository<AlarmMatching, Long> {
}
