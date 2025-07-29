package com.hertz.hertz_be.domain.channel.service.v2;

import com.hertz.hertz_be.domain.channel.dto.request.v2.SignalMatchingRequestDto;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SignalRoomRepository signalRoomRepository;

    @Mock
    private AsyncChannelService asyncChannelService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ChannelService channelService;

    private SignalRoom room;
    private User user;
    private User partner;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).nickname("sender").email("sender@test.com").build();
        partner = User.builder().id(2L).nickname("receiver").email("receiver@test.com").build();
        room = SignalRoom.builder()
                .id(100L)
                .senderUser(user)
                .receiverUser(partner)
                .senderMatchingStatus(MatchingStatus.SIGNAL)
                .receiverMatchingStatus(MatchingStatus.SIGNAL)
                .userPairSignal("sig-1-2")
                .category(Category.FRIEND)
                .build();

        ReflectionTestUtils.setField(channelService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("채널 나가기 - 성공")
    void leaveChannelRoom_success() {
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(userRepository.existsById(1L)).thenReturn(true);

        channelService.leaveChannelRoom(100L, 1L);

        assertTrue(room.isUserExited(1L));
    }

    @Test
    @DisplayName("채널 나가기 - 존재하지 않는 방")
    void leaveChannelRoom_shouldThrow_whenRoomNotFound() {
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            channelService.leaveChannelRoom(100L, 1L);
        });

        assertEquals(ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("채널 나가기 - 존재하지 않는 유저")
    void leaveChannelRoom_shouldThrow_whenUserNotFound() {
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(userRepository.existsById(1L)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            channelService.leaveChannelRoom(100L, 1L);
        });

        assertEquals(UserResponseCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("채널 매칭 상태 업데이트 - 성공 (MATCHED)")
    void channelMatchingStatusUpdate_success_matched() {
        SignalMatchingRequestDto dto = new SignalMatchingRequestDto(100L);
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(signalRoomRepository.updateSenderMatchingStatus(100L, 1L, MatchingStatus.MATCHED)).thenReturn(1);
        when(signalRoomRepository.updateReceiverMatchingStatus(100L, 1L, MatchingStatus.MATCHED)).thenReturn(0);
        when(signalRoomRepository.findMatchResultByUser(1L, 100L)).thenReturn("matched-user-nickname");

        String result = channelService.channelMatchingStatusUpdate(1L, dto, MatchingStatus.MATCHED);

        assertEquals("matched-user-nickname", result);
        verify(entityManager, times(1)).flush();
        verify(asyncChannelService).notifyMatchingResultToPartner(room, 1L, MatchingStatus.MATCHED);
        verify(asyncChannelService).createMatchingAlarm(room, 1L);
    }

    @Test
    @DisplayName("채널 매칭 상태 업데이트 - 성공 (UNMATCHED)")
    void channelMatchingStatusUpdate_success_unmatched() {
        SignalMatchingRequestDto dto = new SignalMatchingRequestDto(100L);
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(signalRoomRepository.updateSenderMatchingStatus(100L, 1L, MatchingStatus.UNMATCHED)).thenReturn(1);
        when(signalRoomRepository.updateReceiverMatchingStatus(100L, 1L, MatchingStatus.UNMATCHED)).thenReturn(0);

        String result = channelService.channelMatchingStatusUpdate(1L, dto, MatchingStatus.UNMATCHED);

        assertEquals(ChannelResponseCode.MATCH_REJECTION_SUCCESS.getCode(), result);
        verify(asyncChannelService).notifyMatchingResultToPartner(room, 1L, MatchingStatus.UNMATCHED);
        verify(asyncChannelService).createMatchingAlarm(room, 1L);
    }

    @Test
    @DisplayName("채널 매칭 상태 업데이트 - 참여 중 아님")
    void channelMatchingStatusUpdate_shouldThrow_whenUpdateCountIsZero() {
        SignalMatchingRequestDto dto = new SignalMatchingRequestDto(100L);
        when(signalRoomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(signalRoomRepository.updateSenderMatchingStatus(100L, 1L, MatchingStatus.MATCHED)).thenReturn(0);
        when(signalRoomRepository.updateReceiverMatchingStatus(100L, 1L, MatchingStatus.MATCHED)).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.channelMatchingStatusUpdate(1L, dto, MatchingStatus.MATCHED);
        });

        assertEquals(ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("채널 매칭 상태 업데이트 - 방 없음")
    void channelMatchingStatusUpdate_shouldThrow_whenRoomNotFound() {
        SignalMatchingRequestDto dto = new SignalMatchingRequestDto(999L);
        when(signalRoomRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.channelMatchingStatusUpdate(1L, dto, MatchingStatus.MATCHED);
        });

        assertEquals(ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(), ex.getCode());
    }
}

