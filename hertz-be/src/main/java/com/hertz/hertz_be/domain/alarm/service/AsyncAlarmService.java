package com.hertz.hertz_be.domain.alarm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsyncAlarmService {
    private final SseAlarmService sseAlarmService;

    @Async
    public void updateAlarmNotification(Long userId) {
        sseAlarmService.updateAlarmNotification(userId);
    }
}
