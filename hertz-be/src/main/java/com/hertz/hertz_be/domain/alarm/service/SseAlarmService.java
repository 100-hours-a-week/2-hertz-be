package com.hertz.hertz_be.domain.alarm.service;

import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SseAlarmService {
    private final UserAlarmRepository userAlarmRepository;
    private final SseService sseService;

    public void updateAlarmNotification(Long userId) {
        boolean isThereNewAlarm = userAlarmRepository.isThereNewAlarm(userId);
        if (isThereNewAlarm) {
            boolean result = sseService.sendToClient(userId, SseEventName.NEW_ALARM.getValue(), "");
            if (result){log.info("[새 알림 존재 SSE 알림 전송] userId={}", userId);}
        } else {
            boolean result = sseService.sendToClient(userId, SseEventName.NO_ANY_NEW_ALARM.getValue(), "");
            if (result){log.info("[새 알림 미존재 SSE 알림 전송] userId={}", userId);}
        }
    }
}
