package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.sse.*;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.sse.SseService;
import com.hertz.hertz_be.global.util.AESUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SseChannelService {
    @Value("${matching.convert.delay-minutes}")
    private long matchingConvertDelayMinutes;

    private final SseService sseService;
    private final UserRepository userRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final AESUtil aesUtil;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final Map<Long, ScheduledFuture<?>> scheduledMap = new ConcurrentHashMap<>();

    public void notifyMatchingConverted(
            Long channelRoomId,
            Long senderId, String senderNickname,
            Long receiverId, String receiverNickname
    ) {
        // 이미 예약되어 있다면 중복 예약 방지
        if (scheduledMap.containsKey(channelRoomId)) {
            return;
        }

        Runnable task = () -> {

            LocalDateTime matchedAt = LocalDateTime.now();

            sendMatchingConvertedSse(senderId, receiverId, receiverNickname, channelRoomId, matchedAt);
            sendMatchingConvertedSse(receiverId, senderId, senderNickname, channelRoomId, matchedAt);

            scheduledMap.remove(channelRoomId);
        };

        ScheduledFuture<?> future = scheduler.schedule(task, matchingConvertDelayMinutes, TimeUnit.MINUTES);
        scheduledMap.put(channelRoomId, future);

        log.info("[매칭 전환 예약] {} ↔ {} 에 대해 {}분 후 SSE 예약 완료", senderId, receiverId,matchingConvertDelayMinutes);
    }

    public void notifyMatchingConvertedInChannelRoom(SignalRoom room, Long userId) {
        User partnerUser = room.getPartnerUser(userId);
        boolean isReceiver = Objects.equals(userId, room.getReceiverUser().getId());

        MatchingStatus userStatus = isReceiver ? room.getReceiverMatchingStatus() : room.getSenderMatchingStatus();
        MatchingStatus partnerStatus = isReceiver ? room.getSenderMatchingStatus() : room.getReceiverMatchingStatus();

        boolean userMatched = (userStatus != MatchingStatus.SIGNAL);
        boolean partnerMatched = (partnerStatus != MatchingStatus.SIGNAL);

        sendMatchingConvertedInChannelRoom(userId, room.getId(), userMatched, partnerMatched, partnerUser.getNickname());
    }

    private void sendMatchingConvertedSse(
            Long targetUserId,
            Long partnerId,
            String partnerNickname,
            Long roomId,
            LocalDateTime matchedAt
    ) {
        MatchingConvertedResponseDto dto = new MatchingConvertedResponseDto(
                roomId,
                matchedAt,
                partnerId,
                partnerNickname
        );

        boolean result = sseService.sendToClient(targetUserId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), dto);
        if (result) {
            log.info("[페이지 상관 없이 매칭 전환 여부 메세지] userId={}, roomId={} 전송 완료", targetUserId, roomId);
        }
    }

    private void sendMatchingConvertedInChannelRoom(
            Long userId,
            Long roomId,
            boolean hasResponded,
            boolean partnerHasResponded,
            String partnerNickName
    ) {
        MatchingConvertedInChannelRoomResponseDto dto = new MatchingConvertedInChannelRoomResponseDto(
                roomId,
                partnerNickName,
                hasResponded,
                partnerHasResponded
        );

        boolean result = sseService.sendToClient(userId, SseEventName.SIGNAL_MATCHING_CONVERSION_IN_ROOM.getValue(), dto);
        if (result) {
            log.info("[채팅방 안에서 매칭 전환 여부 메세지] userId={}, roomId={} 전송 완료", userId, roomId);
        }
    }

    public void updatePartnerChannelList(SignalMessage signalMessage, Long partnerId) {

        String decryptedMessage = aesUtil.decrypt(signalMessage.getMessage());

        ChannelListResponseDto dto = new ChannelListResponseDto(
                signalMessage.getSignalRoom().getId(),
                signalMessage.getSenderUser().getProfileImageUrl(),
                signalMessage.getSenderUser().getNickname(),
                decryptedMessage,
                signalMessage.getSendAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                signalMessage.getIsRead(),
                signalMessage.getSignalRoom().getRelationType()
        );

        boolean result = sseService.sendToClient(partnerId, SseEventName.CHAT_ROOM_UPDATE.getValue(), dto);
        if (result) {
            log.info("[채널 목록 페이지에서 새 메세지에 대한 정보 알림 전송] userId={}, roomId={}", partnerId, signalMessage.getSignalRoom().getId());
        }
    }

    public void updatePartnerNavbar(Long userId) {
        User user = userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new UserException(ResponseCode.USER_NOT_FOUND, "사용자가 존재하지 않습니다."));

        List<SignalRoom> allRooms = Stream.concat(
                user.getSentSignalRooms().stream(),
                user.getReceivedSignalRooms().stream()
        ).collect(Collectors.toList());

        boolean isThereNewMessage = signalMessageRepository.existsBySignalRoomInAndSenderUserNotAndIsReadFalse(allRooms, user);

        if (isThereNewMessage) {
            boolean result = sseService.sendToClient(userId, SseEventName.NAV_NEW_MESSAGE.getValue(), "");
            if (result){log.info("[네비게이션 바에서 새 메세지 알림 전송] userId={}", userId);}
        } else {
            boolean result = sseService.sendToClient(userId, SseEventName.NAV_NO_ANY_NEW_MESSAGE.getValue(), "");
            if (result){log.info("[네비게이션 바에서 새 메세지 없음 알림 전송] userId={}", userId);}
        }
    }

    public void notifyNewMessage(SignalMessage signalMessage, Long partnerId) {
        sendNewSignalOrMessageEvent(signalMessage, partnerId, SseEventName.NEW_MESSAGE_RECEPTION);
    }

    public void notifyNewSignal(SignalMessage signalMessage, Long partnerId) {
        sendNewSignalOrMessageEvent(signalMessage, partnerId, SseEventName.NEW_SIGNAL_RECEPTION);
    }

    private void sendNewSignalOrMessageEvent(SignalMessage signalMessage, Long partnerId, SseEventName eventName) {
        String decryptedMessage = aesUtil.decrypt(signalMessage.getMessage());

        NewMessageResponseDto dto = new NewMessageResponseDto(
                signalMessage.getSignalRoom().getId(),
                signalMessage.getSenderUser().getId(),
                signalMessage.getSenderUser().getNickname(),
                decryptedMessage,
                String.valueOf(signalMessage.getSendAt()),
                signalMessage.getSenderUser().getProfileImageUrl(),
                signalMessage.getSignalRoom().getRelationType()
        );

        boolean result = sseService.sendToClient(partnerId, eventName.getValue(), dto);
        if (result) {
            log.info("[{} 전송] userId={}, roomId={}", eventName.name(), partnerId, signalMessage.getSignalRoom().getId());
        }
    }

    public void notifyMatchingResultToPartner(SignalRoom room, User user, User partner, MatchingStatus matchingStatus) {
        if (matchingStatus == MatchingStatus.MATCHED) {
            boolean result = sendMatchingResultSse(room, user, partner.getId(), SseEventName.MATCHING_SUCCESS);
            if (result){log.info("[{}번 유저에게 매칭 결과 성공 SSE 알림 전송]", partner.getId());}
        }
        else {
            boolean result = sendMatchingResultSse(room, user, partner.getId(), SseEventName.MATCHING_REJECTION);
            if (result){log.info("[{}번 유저에게 매칭 결과 실패 SSE 알림 전송]", partner.getId());}
        }
    }

    public void notifyMatchingConfirmedToPartner(SignalRoom room, User user, User partner) {
        MatchingResultResponseDto dto = new MatchingResultResponseDto(
                room.getId(),
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickname()
        );

        boolean result = sseService.sendToClient(partner.getId(), SseEventName.MATCHING_CONFIRMED.getValue(), dto);
        if (result) {
            log.info("[{}번 유저에게 매칭 여부 SSE 알림 전송]", partner.getId());
        }
    }

    private boolean sendMatchingResultSse(SignalRoom room, User user, Long partnerId, SseEventName sseEventName) {
        MatchingResultResponseDto dto = new MatchingResultResponseDto(
                room.getId(),
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickname()
        );

        return sseService.sendToClient(partnerId, sseEventName.getValue(), dto);
    }

}
