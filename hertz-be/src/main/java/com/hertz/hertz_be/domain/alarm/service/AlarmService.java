package com.hertz.hertz_be.domain.alarm.service;

import static com.hertz.hertz_be.global.util.MessageCreatorUtil.*;
import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
import com.hertz.hertz_be.domain.alarm.dto.response.object.*;
import com.hertz.hertz_be.domain.alarm.entity.*;
import com.hertz.hertz_be.domain.alarm.entity.enums.AlarmCategory;
import com.hertz.hertz_be.domain.alarm.repository.*;
import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlarmService {

    private final EntityManager entityManager;
    private final AlarmNotificationRepository alarmNotificationRepository;
    private final AlarmReportRepository alarmReportRepository;
    private final AlarmMatchingRepository alarmMatchingRepository;
    private final AlarmAlertRepository alarmAlertRepository;
    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;
    private final AsyncAlarmService asyncAlarmService;
    private final SignalRoomRepository signalRoomRepository;
    private final RedissonClient redissonClient;

    @Value("${channel.message.page.size}")
    private int channelMessagePageSize;

    private static final int MAX_LOCK_ATTEMPTS = 3;
    private static final long LOCK_RETRY_DELAY_MS = 2000;

    @Transactional
    public void createNotifyAlarm(CreateNotifyAlarmRequestDto dto, Long userId) {
        User notifyWriter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        "공지 알람 작성 API를 요청한 사용자가 존재하지 않습니다."
                ));

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

        registerAfterCommitCallback(() -> {
            for (User user : allUsers) {
                asyncAlarmService.updateAlarmNotification(user.getId());
            }
        });
    }

    @Transactional
    public void createMatchingAlarm(SignalRoom room, User user, User partner) {
        String lockKey = String.format("lock:alarm-matching:room=%d:user=%d:partner=%d",
                room.getId(), user.getId(), partner.getId());

        for (int attempt = 1; attempt <= MAX_LOCK_ATTEMPTS; attempt++) {
            boolean lockAcquired = redissonClient.getLock(lockKey).tryLock();
            if (lockAcquired) {
                try {
                    boolean alreadyExists = alarmMatchingRepository.existsBySignalRoom(room);
                    if (alreadyExists) return;

                    String alarmTitleForUser;
                    String alarmTitleForPartner;
                    if (Objects.equals(room.getRelationType(), MatchingStatus.UNMATCHED.getValue())) {
                        alarmTitleForUser = createMatchingFailureMessage(partner.getNickname());
                        alarmTitleForPartner = createMatchingFailureMessage(user.getNickname());
                    } else {
                        alarmTitleForUser = createMatchingSuccessMessage(partner.getNickname());
                        alarmTitleForPartner = createMatchingSuccessMessage(user.getNickname());
                    }

                    AlarmMatching alarmMatchingForUser = AlarmMatching.builder()
                            .title(alarmTitleForUser)
                            .partner(partner)
                            .partnerNickname(partner.getNickname())
                            .signalRoom(room)
                            .isMatched(false)
                            .build();
                    AlarmMatching savedAlarmForUser = alarmMatchingRepository.save(alarmMatchingForUser);

                    userAlarmRepository.save(UserAlarm.builder()
                            .alarm(savedAlarmForUser)
                            .user(user)
                            .build());

                    AlarmMatching alarmMatchingForPartner = AlarmMatching.builder()
                            .title(alarmTitleForPartner)
                            .partner(user)
                            .partnerNickname(user.getNickname())
                            .signalRoom(room)
                            .isMatched(false)
                            .build();
                    AlarmMatching savedAlarmForPartner = alarmMatchingRepository.save(alarmMatchingForPartner);

                    userAlarmRepository.save(UserAlarm.builder()
                            .alarm(savedAlarmForPartner)
                            .user(partner)
                            .build());

                    entityManager.flush();

                    registerAfterCommitCallback(() -> {
                        asyncAlarmService.updateAlarmNotification(user.getId());
                        asyncAlarmService.updateAlarmNotification(partner.getId());
                    });

                    return;

                } finally {
                    redissonClient.getLock(lockKey).unlock();
                }
            } else {
                try {
                    Thread.sleep(LOCK_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(
                            NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                            NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                            "매칭 알림을 생성하기 위해 분산 Lock을 기다리는 과정에서 문제 발생."
                    );
                }
            }
        }

        throw new BusinessException(
                NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                "분산 Lock을 통해 매칭 알림을 생성 과정에서 문제 발생."
        );
    }

    @Transactional
    public void createTuningReportAlarm(String emailDomain, int coupleCount) {

        String tuningReportAlarmTitle = createTuningReportMessage();

        AlarmReport alarmReport = AlarmReport.builder()
                .title(tuningReportAlarmTitle)
                .coupleCount(coupleCount)
                .build();

        AlarmReport savedAlarm = alarmReportRepository.save(alarmReport);

        List<User> allUsers = userRepository.findAllByEmailDomain(emailDomain);

        List<UserAlarm> userAlarms = allUsers.stream()
                .map(user -> UserAlarm.builder()
                        .alarm(savedAlarm)
                        .user(user)
                        .build())
                .toList();

        userAlarmRepository.saveAll(userAlarms);

        entityManager.flush();

        registerAfterCommitCallback(() -> {
            for (User user : allUsers) {
                asyncAlarmService.updateAlarmNotification(user.getId());
            }
        });
    }

    @Transactional
    public void createAlertAlarm(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_DEACTIVATED.getCode(),
                        UserResponseCode.USER_DEACTIVATED.getHttpStatus(),
                        UserResponseCode.USER_DEACTIVATED.getMessage()
                ));

        String alarmAlertTitle = createAlertMessageForInappropriateContent();

        AlarmAlert alarmAlert = AlarmAlert.builder()
                .title(alarmAlertTitle)
                .reportedMessage(message)
                .build();

        AlarmAlert savedAlarmForUser = alarmAlertRepository.save(alarmAlert);

        UserAlarm userAlarm = UserAlarm.builder()
                .alarm(savedAlarmForUser)
                .user(user)
                .build();

        userAlarmRepository.save(userAlarm);

        entityManager.flush();

        registerAfterCommitCallback(() -> {
            asyncAlarmService.updateAlarmNotification(user.getId());
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
                        int lastPageNumber = 0;
                        if (channelRoomId != null) {
                            lastPageNumber = signalRoomRepository.findLastPageNumberBySignalRoomId(channelRoomId, channelMessagePageSize);
                        }

                        return new MatchingAlarm(
                                AlarmCategory.MATCHING.getValue(),
                                matching.getTitle(),
                                channelRoomId,
                                matching.getCreatedAt().toString(),
                                lastPageNumber
                        );
                    } else if (alarm instanceof AlarmReport report) {
                        return new ReportAlarm(
                                AlarmCategory.REPORT.getValue(),
                                report.getTitle(),
                                report.getCreatedAt().toString()
                        );
                    } else if (alarm instanceof AlarmAlert alert) {
                        return new AlertAlarm(
                                AlarmCategory.ALERT.getValue(),
                                alert.getTitle(),
                                alert.getCreatedAt().toString()
                        );
                    } else {
                        throw new BusinessException(
                                NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                                "알람 리스트 반환 중 예외 발생했습니다."
                        );
                    }
                })
                .collect(Collectors.toList());

        entityManager.flush();

        registerAfterCommitCallback(() -> {
            asyncAlarmService.updateAlarmNotification(userId);
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
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(
                    UserResponseCode.USER_NOT_FOUND.getCode(),
                    UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                    "알람 삭제 API를 요청한 사용자가 존재하지 않습니다."
            );
        }

        alarmRepository.deleteById(alarmId);
    }

    protected void registerAfterCommitCallback(Runnable callback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                callback.run();
            }
        });
    }
}
