package com.hertz.hertz_be.domain.alarm.service;

import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
import com.hertz.hertz_be.domain.alarm.dto.response.object.AlarmItem;
import com.hertz.hertz_be.domain.alarm.dto.response.object.MatchingAlarm;
import com.hertz.hertz_be.domain.alarm.dto.response.object.NoticeAlarm;
import com.hertz.hertz_be.domain.alarm.dto.response.object.ReportAlarm;
import com.hertz.hertz_be.domain.alarm.entity.*;
import com.hertz.hertz_be.domain.alarm.repository.AlarmNotificationRepository;
import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.exception.InternalServerErrorException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Transactional(readOnly = true)
    public AlarmListResponseDto getAlarmList(int page, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(page, size);
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        Page<UserAlarm> alarms = userAlarmRepository.findRecentUserAlarms(userId, thresholdDate, pageRequest);

        List<AlarmItem> alarmItems = alarms.getContent().stream()
                .map(userAlarm -> {
                    Alarm alarm = userAlarm.getAlarm();

                    if (alarm instanceof AlarmNotification notification) {
                        return new NoticeAlarm(
                                "NOTICE",
                                notification.getTitle(),
                                notification.getContent(),
                                notification.getCreatedAt().toString()
                        );
                    }
                    else if (alarm instanceof AlarmMatching matching) {
                        SignalRoom signalRoom = matching.getSignalRoom();
                        Long channelRoomId = signalRoom.isUserExited(userId) ? null : signalRoom.getId();

                        return new MatchingAlarm(
                                "MATCHING",
                                matching.getTitle(),
                                channelRoomId,
                                matching.getCreatedAt().toString()
                        );
                    } else if (alarm instanceof AlarmReport report) {
                        return new ReportAlarm(
                                "REPORT",
                                report.getTitle(),
                                report.getCreatedAt().toString()
                        );
                    } else {
                        throw new InternalServerErrorException();
                    }
                })
                .collect(Collectors.toList());

        return new AlarmListResponseDto(
                alarmItems,
                alarms.getNumber(),
                alarms.getSize(),
                alarms.isLast()
        );
    }
}
