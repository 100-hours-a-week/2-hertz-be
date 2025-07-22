package com.hertz.hertz_be.domain.alarm.service;

import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.kafka.dto.SseEventDto;
import com.hertz.hertz_be.global.kafka.servise.KafkaProducerService;
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
    private final KafkaProducerService kafkaProducerService;

    public void updateAlarmNotification(Long userId) {
        boolean isThereNewAlarm = userAlarmRepository.isThereNewAlarm(userId);
        if (isThereNewAlarm) {
            kafkaProducerService.sendSseEvent(new SseEventDto(userId, SseEventName.NEW_ALARM.getValue(), ""));
        } else {
            kafkaProducerService.sendSseEvent(new SseEventDto(userId, SseEventName.NO_ANY_NEW_ALARM.getValue(), ""));
        }
    }
}
