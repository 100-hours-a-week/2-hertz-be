package com.hertz.hertz_be.domain.alarm.service;

import static com.hertz.hertz_be.global.util.MessageCreatorUtil.*;
import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
import com.hertz.hertz_be.domain.alarm.dto.response.object.AlarmItem;
import com.hertz.hertz_be.domain.alarm.dto.response.object.MatchingAlarm;
import com.hertz.hertz_be.domain.alarm.dto.response.object.NoticeAlarm;
import com.hertz.hertz_be.domain.alarm.dto.response.object.ReportAlarm;
import com.hertz.hertz_be.domain.alarm.entity.*;
import com.hertz.hertz_be.domain.alarm.entity.enums.AlarmCategory;
import com.hertz.hertz_be.domain.alarm.repository.AlarmMatchingRepository;
import com.hertz.hertz_be.domain.alarm.repository.AlarmNotificationRepository;
import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.repository.AlarmRepository;
import com.hertz.hertz_be.domain.alarm.repository.UserAlarmRepository;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.exception.InternalServerErrorException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlarmService {

    @PersistenceContext
    private EntityManager entityManager;

    private final AlarmNotificationRepository alarmNotificationRepository;
    private final AlarmMatchingRepository alarmMatchingRepository;
    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;
    private final AsyncAlarmService asyncAlarmService;

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

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (User user : allUsers) {
                    asyncAlarmService.updateAlarmNotification(user.getId());
                }
            }
        });
    }

    @Transactional
    public void createMatchingAlarm(SignalRoom room, User user, User partner) {
        String alarmTitleForUser;
        String alarmTitleForPartner;
        if (Objects.equals(room.getRelationType(), MatchingStatus.UNMATCHED.getValue())) {
            alarmTitleForUser = createFailureMessage(partner.getNickname());
            alarmTitleForPartner = createFailureMessage(user.getNickname());
        } else {
            alarmTitleForUser = createSuccessMessage(partner.getNickname());
            alarmTitleForPartner = createSuccessMessage(user.getNickname());
        }

        AlarmMatching alarmMatchingForUser = AlarmMatching.builder()
                .title(alarmTitleForUser)
                .partner(partner)
                .partnerNickname(partner.getNickname())
                .signalRoom(room)
                .isMatched(false)
                .build();
        AlarmMatching savedAlarmForUser = alarmMatchingRepository.save(alarmMatchingForUser);

        UserAlarm userAlarmForUser = UserAlarm.builder()
                .alarm(savedAlarmForUser)
                .user(user)
                .build();

        userAlarmRepository.save(userAlarmForUser);

        AlarmMatching alarmMatchingForPartner = AlarmMatching.builder()
                .title(alarmTitleForPartner)
                .partner(user)
                .partnerNickname(user.getNickname())
                .signalRoom(room)
                .isMatched(false)
                .build();
        AlarmMatching savedAlarmForPartner = alarmMatchingRepository.save(alarmMatchingForPartner);

        UserAlarm userAlarmForPartner = UserAlarm.builder()
                .alarm(savedAlarmForPartner)
                .user(partner)
                .build();

        userAlarmRepository.save(userAlarmForPartner);

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncAlarmService.updateAlarmNotification(user.getId());
                asyncAlarmService.updateAlarmNotification(partner.getId());
            }
        });

    }

    @Transactional
    public AlarmListResponseDto getAlarmList(int page, int size, Long userId) {
        PageRequest pageRequest = PageRequest.of(page, size);
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        Page<UserAlarm> alarms = userAlarmRepository.findRecentUserAlarms(userId, thresholdDate, pageRequest);

        alarms.getContent().stream()
                .filter(userAlarm -> !userAlarm.getIsRead())
                .forEach(UserAlarm::setIsRead);

        List<AlarmItem> alarmItems = alarms.getContent().stream()
                .map(userAlarm -> {
                    Alarm alarm = userAlarm.getAlarm();

                    if (alarm instanceof AlarmNotification notification) {
                        return new NoticeAlarm(
                                AlarmCategory.NOTICE.getValue(),
                                notification.getTitle(),
                                notification.getContent(),
                                notification.getCreatedAt().toString()
                        );
                    }
                    else if (alarm instanceof AlarmMatching matching) {
                        SignalRoom signalRoom = matching.getSignalRoom();
                        Long channelRoomId = (signalRoom != null && !signalRoom.isUserExited(userId)) ? signalRoom.getId() : null;

                        return new MatchingAlarm(
                                AlarmCategory.MATCHING.getValue(),
                                matching.getTitle(),
                                channelRoomId,
                                matching.getCreatedAt().toString()
                        );
                    } else if (alarm instanceof AlarmReport report) {
                        return new ReportAlarm(
                                AlarmCategory.REPORT.getValue(),
                                report.getTitle(),
                                report.getCreatedAt().toString()
                        );
                    } else {
                        throw new InternalServerErrorException();
                    }
                })
                .collect(Collectors.toList());

        entityManager.flush();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                asyncAlarmService.updateAlarmNotification(userId);
            }
        });

        return new AlarmListResponseDto(
                alarmItems,
                alarms.getNumber(),
                alarms.getSize(),
                alarms.isLast()
        );
    }

    @Transactional
    public void deleteAlarm(Long alarmId, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        alarmRepository.deleteById(alarmId);
    }
}
