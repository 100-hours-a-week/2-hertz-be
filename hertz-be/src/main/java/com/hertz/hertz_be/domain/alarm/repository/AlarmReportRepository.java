package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.AlarmReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmReportRepository extends JpaRepository<AlarmReport, Long> {
}
