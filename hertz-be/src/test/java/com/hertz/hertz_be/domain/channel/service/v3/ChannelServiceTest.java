package com.hertz.hertz_be.domain.channel.service.v3;

import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.channel.dto.request.v3.ChatReportRequestDto;
import com.hertz.hertz_be.domain.channel.dto.request.v3.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelListResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.ChannelRoomResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.SendSignalResponseDto;
import com.hertz.hertz_be.domain.channel.dto.response.v3.TuningResponseDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.Tuning;
import com.hertz.hertz_be.domain.channel.entity.TuningResult;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.fixture.SignalMessageFixture;
import com.hertz.hertz_be.domain.channel.fixture.SignalRoomFixture;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningRepository;
import com.hertz.hertz_be.domain.channel.repository.TuningResultRepository;
import com.hertz.hertz_be.domain.channel.repository.projection.RoomWithLastSenderProjection;
import com.hertz.hertz_be.domain.channel.responsecode.ChannelResponseCode;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.interests.repository.UserInterestsRepository;
import com.hertz.hertz_be.domain.interests.service.InterestsService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.auth.fixture.UserFixture;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.infra.ai.client.TuningAiClient;
import com.hertz.hertz_be.global.infra.ai.dto.response.AiChatReportResponseDto;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChannelServiceTest {

    @Mock private SignalRoomRepository signalRoomRepository;
    @Mock private AESUtil aesUtil;
    @Mock private SignalMessageRepository signalMessageRepository;
    @Mock private AsyncChannelService asyncChannelService;
    @Mock private EntityManager entityManager;
    @Mock private UserRepository userRepository;
    @Mock private UserInterestsRepository userInterestsRepository;
    @Mock private TuningRepository tuningRepository;
    @Mock private TuningResultRepository tuningResultRepository;
    @Mock private InterestsService interestsService;
    @Mock private TuningAiClient tuningAiClient;
    @Mock private AlarmService alarmService;

    @InjectMocks
    private ChannelService channelService;

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = UserFixture.createDefaultSender(); // id = 1L
        receiver = UserFixture.createDefaultReceiver(); // id = 2L
        ReflectionTestUtils.setField(channelService, "entityManager", entityManager);
        ReflectionTestUtils.setField(channelService, "channelMessagePageSize", 10);
    }

    @Test
    @DisplayName("getPersonalSignalRoomList - 참여 중 채널만 필터링")
    void getPersonalSignalRoomList_success() {
        SignalRoom room1 = spy(SignalRoomFixture.createWithId(sender, receiver, 100L));
        SignalRoom room2 = mock(SignalRoom.class); // 나간 방

        SignalMessage lastMessage = SignalMessageFixture.createWithId(receiver, "encrypted-msg", 200L);
        when(room1.isUserExited(1L)).thenReturn(false);
        when(room1.getPartnerUser(1L)).thenReturn(receiver);
        when(room1.getMessages()).thenReturn(List.of(lastMessage));

        when(room2.isUserExited(1L)).thenReturn(true);

        when(aesUtil.decrypt("encrypted-msg")).thenReturn("decrypted-msg");

        Page<SignalRoom> roomPage = new PageImpl<>(List.of(room1, room2));
        when(signalRoomRepository.findAllOrderByLastMessageTimeDesc(eq(1L), any(PageRequest.class))).thenReturn(roomPage);

        ChannelListResponseDto result = channelService.getPersonalSignalRoomList(1L, 0, 10);

        assertEquals(1, result.getList().size());
        assertEquals("decrypted-msg", result.getList().get(0).getLastMessage());
    }

    @Test
    @DisplayName("getPersonalSignalRoomList - 메시지가 없는 방 처리")
    void getPersonalSignalRoomList_noMessages() {
        SignalRoom room = spy(SignalRoomFixture.createWithId(sender, receiver, 101L));
        when(room.isUserExited(1L)).thenReturn(false);
        when(room.getPartnerUser(1L)).thenReturn(receiver);
        when(room.getMessages()).thenReturn(List.of());

        Page<SignalRoom> roomPage = new PageImpl<>(List.of(room));
        when(signalRoomRepository.findAllOrderByLastMessageTimeDesc(eq(1L), any(PageRequest.class))).thenReturn(roomPage);

        ChannelListResponseDto result = channelService.getPersonalSignalRoomList(1L, 0, 10);

        assertEquals(1, result.getList().size());
        assertEquals("", result.getList().get(0).getLastMessage());
        assertNull(result.getList().get(0).getLastMessageTime());
    }

    @Test
    @DisplayName("getPersonalSignalRoomList - 복호화 예외 fallback")
    void getPersonalSignalRoomList_decryptionError() {
        SignalRoom room = spy(SignalRoomFixture.createWithId(sender, receiver, 102L));
        SignalMessage lastMessage = SignalMessageFixture.createWithId(receiver, "bad-encrypted-msg", 201L);

        when(room.isUserExited(1L)).thenReturn(false);
        when(room.getPartnerUser(1L)).thenReturn(receiver);
        when(room.getMessages()).thenReturn(List.of(lastMessage));

        when(aesUtil.decrypt("bad-encrypted-msg")).thenThrow(new RuntimeException("decryption failed"));

        Page<SignalRoom> roomPage = new PageImpl<>(List.of(room));
        when(signalRoomRepository.findAllOrderByLastMessageTimeDesc(eq(1L), any(PageRequest.class))).thenReturn(roomPage);

        ChannelListResponseDto result = channelService.getPersonalSignalRoomList(1L, 0, 10);

        assertEquals(1, result.getList().size());
        assertEquals("메세지를 표시할 수 없습니다.", result.getList().get(0).getLastMessage());
    }

    @Test
    @DisplayName("sendSignal - 성공 케이스")
    void sendSignal_success() {
        sender = UserFixture.createDefaultSender();
        receiver = UserFixture.createReceiverThatAllows(Category.FRIEND); // 수정된 부분

        SendSignalRequestDto dto = new SendSignalRequestDto(receiver.getId(), "안녕", Category.FRIEND);
        String encryptedMessage = "암호화된-안녕";

        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));
        when(signalRoomRepository.existsByUserPairAndCategory(sender, receiver, Category.FRIEND)).thenReturn(false);
        when(aesUtil.encrypt("안녕")).thenReturn(encryptedMessage);

        SendSignalResponseDto result = channelService.sendSignal(sender.getId(), dto);

        assertNotNull(result);
        verify(signalRoomRepository).save(any(SignalRoom.class));
        verify(signalMessageRepository).save(any());
        verify(entityManager).flush();
    }

    @Test
    @DisplayName("sendSignal - 유효하지 않은 카테고리 예외")
    void sendSignal_invalidCategory_throwsException() {
        sender = UserFixture.createDefaultSender();
        receiver = UserFixture.createReceiverThatRejects(Category.FRIEND); // 수정된 부분

        SendSignalRequestDto dto = new SendSignalRequestDto(receiver.getId(), "안녕", Category.FRIEND);

        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.sendSignal(sender.getId(), dto)
        );

        assertEquals(UserResponseCode.CATEGORY_IS_REJECTED.getCode(), ex.getCode());
    }

    @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
    @DisplayName("getChannelRoom - 정상 조회")
    void getChannelRoom_success() {
        Long roomId = 100L;
        Long userId = 1L;

        // 1. sender = 유저, receiver = 상대방
        User sender = UserFixture.createDefaultSender();
        User receiver = UserFixture.createDefaultReceiver();
        SignalRoom room = spy(SignalRoomFixture.createWithId(sender, receiver, roomId));
        SignalMessage message = SignalMessageFixture.createWithId(sender, "encrypted-msg", 999L);

        // 2. 채널 조건
        when(signalRoomRepository.findById(roomId)).thenReturn(Optional.of(room));
        doReturn(true).when(room).isParticipant(userId);
        doReturn(false).when(room).isUserExited(userId);
        doReturn(false).when(room).isPartnerExited(userId);
        doReturn(receiver).when(room).getPartnerUser(userId);
        doReturn("친구").when(room).getRelationType();
        doReturn(Category.FRIEND).when(room).getCategory();

        // 3. 상대 유저 정보 존재
        when(userRepository.findByIdAndDeletedAtIsNull(receiver.getId())).thenReturn(Optional.of(receiver));

        // 4. 마지막 보낸 유저: sender
        RoomWithLastSenderProjection lastSender = mock(RoomWithLastSenderProjection.class);
        when(lastSender.getLastSenderId()).thenReturn(2L); // 상대방이 보낸 메시지 → 읽음 처리 필요
        when(signalMessageRepository.findRoomsWithLastSender(roomId)).thenReturn(Optional.of(lastSender));

        // 5. 메시지 페이지 조회
        Page<SignalMessage> messagePage = new PageImpl<>(List.of(message));
        when(signalMessageRepository.findBySignalRoom_Id(eq(roomId), any())).thenReturn(messagePage);

        // 6. 복호화 처리
        when(aesUtil.decrypt("encrypted-msg")).thenReturn("복호화된 메시지");

        // 실행
        ChannelRoomResponseDto result = channelService.getChannelRoom(roomId, userId, 0, 10);

        // 검증
        assertEquals(roomId, result.getChannelRoomId());
        assertEquals("복호화된 메시지", result.getMessages().getList().get(0).getMessageContents());

        verify(signalMessageRepository).markAllMessagesAsReadByRoomId(roomId);
        verify(asyncChannelService).notifyMatchingConvertedInChannelRoom(room, userId);
    }

    @Test
    @DisplayName("getChannelRoom - 참여자가 아닌 경우 예외")
    void getChannelRoom_notParticipant_throwsException() {
        SignalRoom room = mock(SignalRoom.class);
        when(signalRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(room.isParticipant(2L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("getChannelRoom - 유저가 이미 나간 방인 경우 예외")
    void getChannelRoom_userExited_throwsException() {
        SignalRoom room = mock(SignalRoom.class);
        when(signalRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(room.isParticipant(2L)).thenReturn(true);
        when(room.isUserExited(2L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("getChannelRoom - 상대방 유저가 탈퇴한 경우 예외")
    void getChannelRoom_partnerDeleted_throwsException() {
        SignalRoom room = mock(SignalRoom.class);
        User partner = UserFixture.createDefaultReceiver();

        when(signalRoomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(room.isParticipant(2L)).thenReturn(true);
        when(room.isUserExited(2L)).thenReturn(false);
        when(room.getPartnerUser(2L)).thenReturn(partner);
        when(userRepository.findByIdAndDeletedAtIsNull(partner.getId())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            channelService.getChannelRoom(1L, 2L, 0, 10);
        });
    }

    @Test
    @DisplayName("getTunedUser - 성공")
    void getTunedUser_success() {
        Long userId = 1L;
        String category = "friend";
        User requester = UserFixture.create(userId, "요청자", "requester@test.com");
        User matchedUser = UserFixture.create(2L, "매칭유저", "matched@test.com");
        Tuning tuning = Tuning.builder().user(requester).category(Category.FRIEND).build();
        TuningResult tuningResult = TuningResult.builder().tuning(tuning).matchedUser(matchedUser).lineup(1).build();

        // 1. 유저 & 관심사
        when(userRepository.findById(userId)).thenReturn(Optional.of(requester));
        when(userInterestsRepository.existsByUser(requester)).thenReturn(true);

        // 2. 튜닝 존재 & 결과 존재
        when(tuningRepository.findByUserAndCategory(requester, Category.FRIEND)).thenReturn(Optional.of(tuning));
        when(tuningResultRepository.existsByTuning(tuning)).thenReturn(true);
        when(tuningResultRepository.findFirstByTuningOrderByLineupAsc(tuning)).thenReturn(Optional.of(tuningResult));

        // 3. 관심사 정보
        when(interestsService.getUserKeywords(matchedUser.getId())).thenReturn(Map.of("성격", "활발함"));
        when(interestsService.getUserInterests(anyLong())).thenReturn(Map.of("취미", List.of("영화", "산책")));
        when(interestsService.extractSameInterests(any(), any())).thenReturn(Map.of("취미", List.of("산책")));

        // when
        TuningResponseDto dto = channelService.getTunedUser(userId, category);

        // then
        assertNotNull(dto);
        assertEquals(matchedUser.getId(), dto.userId());
        assertEquals(matchedUser.getNickname(), dto.nickname());

        verify(tuningResultRepository).delete(tuningResult);
    }

    @Test
    @DisplayName("getTunedUser - 유저가 존재하지 않는 경우 예외")
    void getTunedUser_userNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.getTunedUser(1L, "friend")
        );

        assertEquals(UserResponseCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("getTunedUser - 관심사 미선택 예외")
    void getTunedUser_noInterests_throwsException() {
        User user = UserFixture.createDefaultSender();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userInterestsRepository.existsByUser(user)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.getTunedUser(user.getId(), "friend")
        );

        assertEquals(ChannelResponseCode.USER_INTERESTS_NOT_SELECTED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reportMessage - AI가 유해하다고 판단하면 알림 생성")
    void reportMessage_toxicMessage_shouldCreateAlarm() {
        Long reporterId = 1L;
        Long reportedUserId = 2L;
        int messageId = 100;
        String content = "욕설 포함된 메시지";

        ChatReportRequestDto requestDto = new ChatReportRequestDto(messageId, content, reportedUserId);

        when(userRepository.existsById(reporterId)).thenReturn(true);
        when(userRepository.existsById(reportedUserId)).thenReturn(true);

        AiChatReportResponseDto aiResponse = new AiChatReportResponseDto(
                ChannelResponseCode.CENSORED_SUCCESS.name(),
                Map.of("result", true)
        );
        when(tuningAiClient.sendChatReport(requestDto)).thenReturn(aiResponse);

        channelService.reportMessage(reporterId, requestDto);

        verify(alarmService).createAlertAlarm(reportedUserId, content);
    }

    @Test
    @DisplayName("reportMessage - 신고자 유저가 존재하지 않으면 예외")
    void reportMessage_reporterNotExists_shouldThrow() {
        Long reporterId = 1L;
        ChatReportRequestDto requestDto = new ChatReportRequestDto(123, "신고 메시지", 2L);

        when(userRepository.existsById(reporterId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.reportMessage(reporterId, requestDto)
        );

        assertEquals(UserResponseCode.USER_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reportMessage - 피신고자 유저가 존재하지 않으면 예외")
    void reportMessage_reportedUserNotExists_shouldThrow() {
        Long reporterId = 1L;
        Long reportedUserId = 2L;
        ChatReportRequestDto requestDto = new ChatReportRequestDto(123, "신고 메시지", reportedUserId);

        when(userRepository.existsById(reporterId)).thenReturn(true);
        when(userRepository.existsById(reportedUserId)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                channelService.reportMessage(reporterId, requestDto)
        );

        assertEquals(UserResponseCode.USER_DEACTIVATED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("reportMessage - AI가 유해하지 않다고 판단하면 알림 생성 안함")
    void reportMessage_nonToxicMessage_shouldNotCreateAlarm() {
        Long reporterId = 1L;
        Long reportedUserId = 2L;
        ChatReportRequestDto requestDto = new ChatReportRequestDto(123, "정상적인 메시지", reportedUserId);

        when(userRepository.existsById(reporterId)).thenReturn(true);
        when(userRepository.existsById(reportedUserId)).thenReturn(true);

        AiChatReportResponseDto aiResponse = new AiChatReportResponseDto(
                ChannelResponseCode.CENSORED_SUCCESS.name(),
                Map.of("result", false)
        );
        when(tuningAiClient.sendChatReport(requestDto)).thenReturn(aiResponse);

        channelService.reportMessage(reporterId, requestDto);

        verify(alarmService, never()).createAlertAlarm(anyLong(), anyString());
    }
}
