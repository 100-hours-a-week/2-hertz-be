package com.hertz.hertz_be.domain.channel.service.v1;

import com.hertz.hertz_be.domain.auth.fixture.UserFixture;
import com.hertz.hertz_be.domain.channel.dto.request.v1.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.request.v3.SendMessageRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v1.TuningResponseDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.TuningResult;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.fixture.SignalMessageFixture;
import com.hertz.hertz_be.domain.channel.fixture.SignalRoomFixture;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningResultRepository;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.util.AESUtil;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SignalRoomRepository signalRoomRepository;
    @Mock private SignalMessageRepository signalMessageRepository;
    @Mock private AESUtil aesUtil;
    @Mock private AsyncChannelService asyncChannelService;
    @Mock private EntityManager entityManager;
    @Mock private TuningRepository tuningRepository;
    @Mock private TuningResultRepository tuningResultRepository;
    @Mock private UserInterestsRepository userInterestsRepository;
    @Mock private InterestsService interestsService;

    @InjectMocks
    private ChannelService channelService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setup() {
        sender = User.builder().id(1L).nickname("sender").email("sender@test.com").build();
        receiver = User.builder().id(2L).nickname("receiver").email("receiver@test.com").build();

        ReflectionTestUtils.setField(channelService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("sendSignal - 성공 케이스")
    void sendSignal_success() {
        // given
        SendSignalRequestDto dto = new SendSignalRequestDto(2L, "Hello");
        String encryptedMsg = "encrypted-hello";

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(signalRoomRepository.findByUserPairSignal("1_2")).thenReturn(Optional.empty());
        when(aesUtil.encrypt("Hello")).thenReturn(encryptedMsg);

        // when
        SendSignalResponseDto result = channelService.sendSignal(1L, dto);

        // then
        assertNotNull(result);
        verify(signalRoomRepository).save(any(SignalRoom.class));
        verify(signalMessageRepository).save(any(SignalMessage.class));
        verify(entityManager).flush();
        verify(asyncChannelService).sendNewMessageNotifyToPartner(any(), any(), eq(2L), eq(true));
    }

    @Test
    @DisplayName("sendSignal - 존재하지 않는 수신자 예외")
    void sendSignal_shouldThrow_whenReceiverNotFound() {
        SendSignalRequestDto dto = new SendSignalRequestDto(2L, "Hello");

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.sendSignal(1L, dto)
        );

        assertEquals(UserResponseCode.USER_DEACTIVATED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendSignal - 자기 자신에게 보낼 경우 예외")
    void sendSignal_shouldThrow_whenSelfSignal() {
        SendSignalRequestDto dto = new SendSignalRequestDto(1L, "Hi");
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.sendSignal(1L, dto)
        );

        assertEquals(ChannelResponseCode.ALREADY_IN_CONVERSATION.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getTunedUser - 성공")
    void getTunedUser_success() {
        Long userId = 1L;
        User requester = UserFixture.create(userId, "요청자", "requester@test.com");
        User matched = UserFixture.create(2L, "상대방", "matched@test.com");
        Tuning tuning = Tuning.builder().user(requester).build();
        TuningResult result = TuningResult.builder().matchedUser(matched).tuning(tuning).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(userInterestsRepository.existsByUser(requester)).thenReturn(true);
        when(tuningRepository.findByUserAndCategory(requester, Category.FRIEND)).thenReturn(Optional.of(tuning));
        when(tuningResultRepository.existsByTuning(tuning)).thenReturn(true);
        when(tuningResultRepository.findFirstByTuningOrderByLineupAsc(tuning)).thenReturn(Optional.of(result));

        when(interestsService.getUserKeywords(matched.getId())).thenReturn(Map.of());
        when(interestsService.getUserInterests(anyLong())).thenReturn(Map.of());
        when(interestsService.extractSameInterests(any(), any())).thenReturn(Map.of());

        TuningResponseDto dto = channelService.getTunedUser(userId);

        assertEquals(matched.getId(), dto.userId());
        verify(tuningResultRepository).delete(result);
    }

    @Test
    @DisplayName("getTunedUser - 존재하지 않는 사용자 예외")
    void getTunedUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.getTunedUser(1L);
        });

        assertEquals(UserResponseCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getTunedUser - 관심사 미선택 예외")
    void getTunedUser_shouldThrow_whenNoInterestsSelected() {
        User user = UserFixture.create(1L, "요청자", "requester@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userInterestsRepository.existsByUser(user)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.getTunedUser(1L);
        });

        assertEquals(ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendChannelMessage - 성공")
    void sendChannelMessage_success() {
        Long roomId = 1L, userId = 2L;
        String encrypted = "암호화된 메세지";

        User user = UserFixture.create(userId, "user", "user@test.com");
        User partner = UserFixture.create(3L, "partner", "partner@test.com");
        SignalRoom room = mock(SignalRoom.class);
        when(room.isParticipant(userId)).thenReturn(true);
        when(room.getPartnerUser(userId)).thenReturn(partner);

        SendMessageRequestDto dto = new SendMessageRequestDto("hello");

        when(signalRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.findByIdAndDeletedAtIsNull(partner.getId())).thenReturn(Optional.of(partner));
        when(aesUtil.encrypt("hello")).thenReturn(encrypted);

        channelService.sendChannelMessage(roomId, userId, dto);

        verify(signalMessageRepository).save(any(SignalMessage.class));
        verify(entityManager).flush();
        verify(asyncChannelService).sendNewMessageNotifyToPartner(eq(room), any(), eq(partner.getId()), eq(false));
    }

    @Test
    @DisplayName("sendChannelMessage - 채팅방 없음 예외")
    void sendChannelMessage_shouldThrow_whenRoomNotFound() {
        when(signalRoomRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.sendChannelMessage(1L, 2L, new SendMessageRequestDto("hi"));
        });

        assertEquals(ChannelResponseCode.CHANNEL_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("sendChannelMessage - 유저가 채팅방 참가자가 아님")
    void sendChannelMessage_shouldThrow_whenNotParticipant() {
        User user = UserFixture.create(1L, "user", "user@test.com");
        SignalRoom room = mock(SignalRoom.class);
        when(room.isParticipant(1L)).thenReturn(false);

        when(signalRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            channelService.sendChannelMessage(1L, 1L, new SendMessageRequestDto("msg"));
        });

        assertEquals(ChannelResponseCode.ALREADY_EXITED_CHANNEL_ROOM.getCode(), ex.getCode());
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("채널 조회 - 정상 동작")
    void getChannelRoom_success() {
        Long roomId = 1L, userId = 10L;
        SignalRoom room = mock(SignalRoom.class);
        User partner = mock(User.class);
        User senderUser = mock(User.class);
        SignalMessage message = mock(SignalMessage.class);
        Page<SignalMessage> page = new PageImpl<>(List.of(message));
        RoomWithLastSenderProjection lastSender = mock(RoomWithLastSenderProjection.class);

        // Room과 사용자 설정
        when(signalRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(room.isParticipant(userId)).thenReturn(true);
        when(room.isUserExited(userId)).thenReturn(false);
        when(room.getPartnerUser(userId)).thenReturn(partner);
        when(partner.getId()).thenReturn(20L);
        when(userRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(partner));
        when(room.isPartnerExited(userId)).thenReturn(false);
        when(room.getRelationType()).thenReturn("친구");

        // 메시지 설정
        when(signalMessageRepository.findRoomsWithLastSender(roomId)).thenReturn(Optional.of(lastSender));
        when(lastSender.getLastSenderId()).thenReturn(20L);
        when(signalMessageRepository.findBySignalRoom_Id(eq(roomId), any())).thenReturn(page);

        when(message.getId()).thenReturn(100L);
        when(message.getMessage()).thenReturn("encrypted");
        when(message.getSendAt()).thenReturn(LocalDateTime.now());
        when(message.getIsRead()).thenReturn(true);
        when(message.getSenderUser()).thenReturn(senderUser);
        when(senderUser.getId()).thenReturn(20L);
        when(aesUtil.decrypt("encrypted")).thenReturn("decrypted");

        // 실행
        ChannelRoomResponseDto result = channelService.getChannelRoom(roomId, userId, 0, 10);

        // 검증
        assertEquals(roomId, result.getChannelRoomId());
        assertEquals("decrypted", result.getMessages().getList().get(0).getMessageContents());
        verify(asyncChannelService).notifyMatchingConvertedInChannelRoom(room, userId);
    }



    @Test
    @DisplayName("채널 조회 실패 - 참여하지 않은 채팅방")
    void getChannelRoom_shouldThrow_whenNotParticipant() {
        when(signalRoomRepository.findById(any())).thenReturn(Optional.of(mock(SignalRoom.class)));
        when(signalRoomRepository.findById(any()).get().isParticipant(anyLong())).thenReturn(false);

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("채널 조회 실패 - 유저가 이미 나간 채팅방")
    void getChannelRoom_shouldThrow_whenUserExited() {
        SignalRoom room = mock(SignalRoom.class);
        when(signalRoomRepository.findById(any())).thenReturn(Optional.of(room));
        when(room.isParticipant(anyLong())).thenReturn(true);
        when(room.isUserExited(anyLong())).thenReturn(true);

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("채널 조회 실패 - 상대방 유저 정보 없음")
    void getChannelRoom_shouldThrow_whenPartnerDeactivated() {
        SignalRoom room = mock(SignalRoom.class);
        User partner = mock(User.class);

        when(signalRoomRepository.findById(any())).thenReturn(Optional.of(room));
        when(room.isParticipant(anyLong())).thenReturn(true);
        when(room.isUserExited(anyLong())).thenReturn(false);
        when(room.getPartnerUser(anyLong())).thenReturn(partner);
        when(partner.getId()).thenReturn(99L);
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("개인 채널 목록 조회 - 참여 중 채널만 필터링")
    void getPersonalSignalRoomList_success() {
        Long userId = 1L;

        // 유저 및 파트너 설정
        User sender = UserFixture.createDefaultSender();  // id = 1L
        User partner = UserFixture.create(2L, "partner", "partner@test.com");

        // 참여 중인 채널(room1), 나간 채널(room2)
        SignalRoom realRoom1 = SignalRoomFixture.createWithId(sender, partner, 100L);
        SignalRoom room1 = spy(realRoom1);
        SignalRoom room2 = mock(SignalRoom.class);

        SignalMessage lastMessage = SignalMessageFixture.createWithId(partner, "encrypted-msg", 200L);

        // room1 동작 spy + stubbing
        doReturn(false).when(room1).isUserExited(userId);
        doReturn(partner).when(room1).getPartnerUser(userId);
        doReturn(List.of(lastMessage)).when(room1).getMessages();

        // room2는 나간 채널
        when(room2.isUserExited(userId)).thenReturn(true);

        // AESUtil 동작 mock
        when(aesUtil.decrypt("encrypted-msg")).thenReturn("decrypted-msg");

        Page<SignalRoom> roomPage = new PageImpl<>(List.of(room1, room2));
        when(signalRoomRepository.findAllOrderByLastMessageTimeDesc(eq(userId), any())).thenReturn(roomPage);

        // when
        ChannelListResponseDto result = channelService.getPersonalSignalRoomList(userId, 0, 10);

        // then
        assertEquals(1, result.getList().size());
        assertEquals("decrypted-msg", result.getList().get(0).getLastMessage());
    }
}
