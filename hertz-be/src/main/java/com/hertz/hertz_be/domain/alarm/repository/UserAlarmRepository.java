package com.hertz.hertz_be.domain.alarm.repository;

import com.hertz.hertz_be.domain.alarm.entity.UserAlarm;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface UserAlarmRepository extends JpaRepository<UserAlarm, Long> {

    @Query("""
        SELECT ua
        FROM UserAlarm ua
        JOIN FETCH ua.alarm a
        WHERE ua.user.id = :userId
          AND a.createdAt >= :thresholdDate
        ORDER BY a.createdAt DESC
    """)
    Page<UserAlarm> findRecentUserAlarms(
            @Param("userId") Long userId,
            @Param("thresholdDate") LocalDateTime thresholdDate,
            Pageable pageable
    );

    @Query("SELECT CASE WHEN COUNT(ua) > 0 THEN true ELSE false END " +
            "FROM UserAlarm ua WHERE ua.user.id = :userId AND ua.isRead = false")
    boolean isThereNewAlarm(@Param("userId") Long userId);
}
