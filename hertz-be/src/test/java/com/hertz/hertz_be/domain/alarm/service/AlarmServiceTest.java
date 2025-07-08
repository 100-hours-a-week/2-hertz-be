package com.hertz.hertz_be.domain.alarm.service;

import com.hertz.hertz_be.domain.alarm.dto.request.CreateNotifyAlarmRequestDto;
import com.hertz.hertz_be.domain.alarm.dto.response.AlarmListResponseDto;
import com.hertz.hertz_be.domain.alarm.entity.*;
import com.hertz.hertz_be.domain.alarm.repository.*;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.domain.channel.fixture.SignalRoomFixture;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock private AlarmNotificationRepository alarmNotificationRepository;
    @Mock private AlarmReportRepository alarmReportRepository;
    @Mock private AlarmMatchingRepository alarmMatchingRepository;
    @Mock private AlarmAlertRepository alarmAlertRepository;
    @Mock private AlarmRepository alarmRepository;
    @Mock private UserAlarmRepository userAlarmRepository;
    @Mock private UserRepository userRepository;
    @Mock private AsyncAlarmService asyncAlarmService;
    @Mock private EntityManager entityManager;

    @InjectMocks private AlarmService alarmService;

    private User user;
    private User partner;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).nickname("유저").email("user@domain.com").build();
        partner = User.builder().id(2L).nickname("파트너").email("partner@domain.com").build();

        alarmService = spy(alarmService);
        lenient().doNothing().when(alarmService).registerAfterCommitCallback(any());
    }

    @Test
    @DisplayName("공지 알람 생성 - 성공")
    void createNotifyAlarm_success() {
        CreateNotifyAlarmRequestDto dto = new CreateNotifyAlarmRequestDto("제목", "내용");
        AlarmNotification savedAlarm = AlarmNotification.builder().id(1L).title("제목").content("내용").writer(user).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(alarmNotificationRepository.save(any())).thenReturn(savedAlarm);
        when(userRepository.findAll()).thenReturn(List.of(user, partner));

        alarmService.createNotifyAlarm(dto, 1L);

        verify(userRepository, times(1)).findById(1L);
        verify(alarmNotificationRepository, times(1)).save(any());
        verify(userAlarmRepository, times(1)).saveAll(anyList());
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("공지 알람 생성 - 유저 없음")
    void createNotifyAlarm_userNotFound() {
        CreateNotifyAlarmRequestDto dto = new CreateNotifyAlarmRequestDto("제목", "내용");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> alarmService.createNotifyAlarm(dto, 1L));

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("매칭 알람 생성 - 매칭 성공")
    void createMatchingAlarm_success() {
        SignalRoom room = SignalRoomFixture.createMatchedRoom(user, partner);

        when(alarmMatchingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        alarmService.createMatchingAlarm(room, user, partner);

        verify(alarmMatchingRepository, times(2)).save(any());
        verify(userAlarmRepository, times(2)).save(any());
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("매칭 알람 생성 - 매칭 실패")
    void createMatchingAlarm_failed() {
        SignalRoom room = SignalRoomFixture.createUnmatchedRoom(user, partner);

        when(alarmMatchingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        alarmService.createMatchingAlarm(room, user, partner);

        verify(alarmMatchingRepository, times(2)).save(any());
        verify(userAlarmRepository, times(2)).save(any());
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("튜닝 리포트 알람 생성 - 성공")
    void createTuningReportAlarm_success() {
        when(alarmReportRepository.save(any())).thenReturn(AlarmReport.builder().id(1L).title("보고서").build());
        when(userRepository.findAllByEmailDomain("domain.com")).thenReturn(List.of(user, partner));

        alarmService.createTuningReportAlarm("domain.com", 5);

        verify(alarmReportRepository, times(1)).save(any());
        verify(userAlarmRepository, times(1)).saveAll(anyList());
        verify(entityManager, times(1)).flush();
    }


    @Test
    @DisplayName("경고 알람 생성 - 성공")
    void createAlertAlarm_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(alarmAlertRepository.save(any())).thenReturn(AlarmAlert.builder().id(1L).title("경고").reportedMessage("부적절").build());

        alarmService.createAlertAlarm(1L, "부적절");

        verify(alarmAlertRepository, times(1)).save(any());
        verify(userAlarmRepository, times(1)).save(any());
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("경고 알람 생성 - 유저 비활성화")
    void createAlertAlarm_userDeactivated() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> alarmService.createAlertAlarm(1L, "부적절"));

        assertEquals("USER_DEACTIVATED", exception.getCode());
    }

    @Test
    @DisplayName("알람 리스트 조회 - 성공")
    void getAlarmList_success() {
        UserAlarm ua1 = UserAlarm.builder()
                .alarm(AlarmNotification.builder().title("공지").content("내용").createdAt(LocalDateTime.now()).build())
                .isRead(false).build();
        UserAlarm ua2 = UserAlarm.builder()
                .alarm(AlarmAlert.builder().title("경고").createdAt(LocalDateTime.now()).build())
                .isRead(true).build();
        List<UserAlarm> alarmList = List.of(ua1, ua2);

        Page<UserAlarm> page = new PageImpl<>(alarmList);
        when(userAlarmRepository.findRecentUserAlarms(eq(1L), any(), any(PageRequest.class))).thenReturn(page);

        AlarmListResponseDto response = alarmService.getAlarmList(0, 10, 1L);

        assertEquals(2, response.list().size());
        verify(entityManager, times(1)).flush();
    }

    @Test
    @DisplayName("알람 삭제 - 유저 없음")
    void deleteAlarm_userNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> alarmService.deleteAlarm(10L, 1L));

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("알람 삭제 - 성공")
    void deleteAlarm_success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        alarmService.deleteAlarm(10L, 1L);

        verify(alarmRepository, times(1)).deleteById(10L);
    }
}
