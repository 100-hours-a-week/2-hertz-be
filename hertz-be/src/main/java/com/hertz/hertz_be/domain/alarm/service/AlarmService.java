package com.hertz.hertz_be.domain.alarm.service;

import com.hertz.hertz_be.domain.alarm.entity.AlarmNotification;
import com.hertz.hertz_be.domain.alarm.entity.UserAlarm;
import com.hertz.hertz_be.domain.alarm.repository.AlarmNotificationRepository;
import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlarmService {
    private final AlarmNotificationRepository alarmNotificationRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotifyAlarm(CreateNotifyAlarmRequestDto dto, Long userId) {
        User notifyWriter = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        AlarmNotification alarmNotification = AlarmNotification.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .writer(notifyWriter)
                .build();

        AlarmNotification savedAlarm = alarmNotificationRepository.save(alarmNotification);

        List<User> allUsers = userRepository.findAll();

        List<UserAlarm> userAlarms = allUsers.stream()
                .map(user -> UserAlarm.builder()
                        .alarm(savedAlarm)
                        .user(user)
                        .build())
                .toList();

        userAlarmRepository.saveAll(userAlarms);
    }
}
