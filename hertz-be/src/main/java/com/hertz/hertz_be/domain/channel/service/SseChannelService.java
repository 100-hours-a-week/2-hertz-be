package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.sse.*;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.kafka.dto.SseEventDto;
import com.hertz.hertz_be.global.kafka.servise.KafkaProducerService;
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

    private final UserRepository userRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final AESUtil aesUtil;
    private final KafkaProducerService kafkaProducerService;

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

        kafkaProducerService.sendSseEvent(new SseEventDto(targetUserId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), dto));
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

        kafkaProducerService.sendSseEvent(new SseEventDto(userId, SseEventName.SIGNAL_MATCHING_CONVERSION_IN_ROOM.getValue(), dto));
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

        kafkaProducerService.sendSseEvent(new SseEventDto(partnerId, SseEventName.CHAT_ROOM_UPDATE.getValue(), dto));
    }

    public void updatePartnerNavbar(Long userId) {
        User user = userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));

        List<SignalRoom> allRooms = Stream.concat(
                user.getSentSignalRooms().stream(),
                user.getReceivedSignalRooms().stream()
        ).collect(Collectors.toList());

        boolean isThereNewMessage = signalMessageRepository.existsBySignalRoomInAndSenderUserNotAndIsReadFalse(allRooms, user);

        if (isThereNewMessage) {
            kafkaProducerService.sendSseEvent(new SseEventDto(userId, SseEventName.NAV_NEW_MESSAGE.getValue(), ""));
        } else {
            kafkaProducerService.sendSseEvent(new SseEventDto(userId, SseEventName.NAV_NO_ANY_NEW_MESSAGE.getValue(), ""));
        }
    }

    public void notifyNewMessage(int lastPageNumber, SignalMessage signalMessage, Long partnerId) {
        sendNewSignalOrMessageEvent(lastPageNumber,signalMessage, partnerId, SseEventName.NEW_MESSAGE_RECEPTION);
    }

    public void notifyNewSignal(int lastPageNumber, SignalMessage signalMessage, Long partnerId) {
        sendNewSignalOrMessageEvent(lastPageNumber,signalMessage, partnerId, SseEventName.NEW_SIGNAL_RECEPTION);
    }

    private void sendNewSignalOrMessageEvent(int lastPageNumber, SignalMessage signalMessage, Long partnerId, SseEventName eventName) {
        String decryptedMessage = aesUtil.decrypt(signalMessage.getMessage());

        NewMessageResponseDto dto = new NewMessageResponseDto(
                signalMessage.getSignalRoom().getId(),
                signalMessage.getSenderUser().getId(),
                signalMessage.getSenderUser().getNickname(),
                decryptedMessage,
                String.valueOf(signalMessage.getSendAt()),
                signalMessage.getSenderUser().getProfileImageUrl(),
                signalMessage.getSignalRoom().getRelationType(),
                lastPageNumber
        );

        kafkaProducerService.sendSseEvent(new SseEventDto(partnerId, eventName.getValue(), dto));
    }

    public void notifyMatchingResultToPartner(SignalRoom room, User user, User partner, MatchingStatus matchingStatus) {
        if (matchingStatus == MatchingStatus.MATCHED) {
            sendMatchingResultSse(room, user, partner.getId(), SseEventName.MATCHING_SUCCESS);
        }
        else {
            sendMatchingResultSse(room, user, partner.getId(), SseEventName.MATCHING_REJECTION);
        }
    }

    public void notifyMatchingConfirmedToPartner(SignalRoom room, User user, User partner) {
        MatchingResultResponseDto dto = new MatchingResultResponseDto(
                room.getId(),
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickname()
        );

        kafkaProducerService.sendSseEvent(new SseEventDto(partner.getId(), SseEventName.MATCHING_CONFIRMED.getValue(), dto));
    }

    private void sendMatchingResultSse(SignalRoom room, User user, Long partnerId, SseEventName sseEventName) {
        MatchingResultResponseDto dto = new MatchingResultResponseDto(
                room.getId(),
                user.getId(),
                user.getProfileImageUrl(),
                user.getNickname()
        );

        kafkaProducerService.sendSseEvent(new SseEventDto(partnerId, sseEventName.getValue(), dto));
    }

}
