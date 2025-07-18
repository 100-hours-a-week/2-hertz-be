package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.alarm.service.AlarmService;
import com.hertz.hertz_be.domain.channel.dto.object.UserMessageCountDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.webpush.responsecode.FCMEventType;
import com.hertz.hertz_be.global.webpush.service.FCMService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsyncChannelService {

    @Value("${matching.convert.delay-minutes}")
    private long matchingConvertDelayMinutes;
    private final long ONE_MESSAGE = 1L;

    //Todo: 2차 압데이트 전에 장확한 날짜로 수정
    private static final LocalDateTime VERSION_2_UPDATE_DATE = LocalDateTime.of(2025, 6, 20, 12, 0);

    @PersistenceContext
    private EntityManager entityManager;

    private final SignalMessageRepository signalMessageRepository;
    private final SseChannelService sseChannelService;
    private final FCMService fcmService;
    private final UserRepository userRepository;
    private final AlarmService alarmService;

    @Async
    public void notifyMatchingConverted(SignalRoom room) {
        if (room.getReceiverMatchingStatus() != MatchingStatus.SIGNAL && room.getSenderMatchingStatus() != MatchingStatus.SIGNAL) {
            return;
        }

        List<UserMessageCountDto> counts = signalMessageRepository
                .countMessagesBySenderInRoomAfter(room.getId(), VERSION_2_UPDATE_DATE);

        Map<Long, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        UserMessageCountDto::userId,
                        UserMessageCountDto::messageCount
                ));

        if (shouldNotifyMatchingConverted(room, countMap)) {
            sseChannelService.notifyMatchingConverted(
                    room.getId(),
                    room.getSenderUser().getId(), room.getSenderUser().getNickname(),
                    room.getReceiverUser().getId(), room.getReceiverUser().getNickname()
            );

        if(fcmService.shouldNotify(FCMEventType.MATCHING_CONVERTED, room.getId())) {
            fcmService.sendWebPush(room.getSenderUser().getId(), "매칭 상태 전환",room.getReceiverUser().getNickname() + "님과 매칭이 가능합니다 🎉");
            fcmService.sendWebPush(room.getReceiverUser().getId(), "매칭 상태 전환",room.getSenderUser().getNickname() + "님과 매칭이 가능합니다 🎉");
        }

        }
    }

    private boolean shouldNotifyMatchingConverted(SignalRoom room, Map<Long, Long> countMap) {
        Long receiverId = room.getReceiverUser().getId();
        Long receiverMessageCount = countMap.getOrDefault(receiverId, 0L);
        return receiverMessageCount >= ONE_MESSAGE;
    }

    @Async
    public void notifyMatchingConvertedInChannelRoom(SignalRoom room, Long userId) {
        if (room.getReceiverMatchingStatus() != MatchingStatus.SIGNAL && room.getSenderMatchingStatus() != MatchingStatus.SIGNAL) {
            return;
        }

        Long roomId = room.getId();

        Long receiverId = room.getReceiverUser().getId();

        List<SignalMessage> messages = signalMessageRepository
                .findBySignalRoomIdAndSenderUserIdAndSendAtAfterOrderBySendAtAsc(roomId, receiverId, VERSION_2_UPDATE_DATE);

        if (messages.isEmpty()) return;

        SignalMessage firstMessage = messages.get(0);
        LocalDateTime sentTime = firstMessage.getSendAt();

        if (sentTime.plusMinutes(matchingConvertDelayMinutes).isBefore(LocalDateTime.now())) {
            sseChannelService.notifyMatchingConvertedInChannelRoom(room, userId);
        }
    }

    @Async
    @Transactional
    public void sendNewMessageNotifyToPartner(SignalMessage signalMessage, Long partnerId, boolean isSignal) {
        // signalMessage는 detached 상태 → DB에서 최신 상태 조회
        SignalMessage latestMessageForm = signalMessageRepository.findById(signalMessage.getId())
                .orElseThrow(() -> new BusinessException(
                        NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                        NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus(),
                        NewResponseCode.INTERNAL_SERVER_ERROR.getMessage()
                ));

        if (!latestMessageForm.getIsRead()) {
            sseChannelService.updatePartnerChannelList(latestMessageForm, partnerId);
            if (isSignal) {
                sseChannelService.notifyNewSignal(latestMessageForm, partnerId);
            } else {
                sseChannelService.notifyNewMessage(latestMessageForm, partnerId);
            }
        }

        sseChannelService.updatePartnerNavbar(partnerId);
    }

    @Async
    @Transactional
    public void updateNavbarMessageNotification(Long userId) {
        sseChannelService.updatePartnerNavbar(userId);
    }

    @Async
    @Transactional
    public void notifyMatchingResultToPartner(SignalRoom room, Long userId, MatchingStatus matchingStatus) {
        SignalRoom latestRoomForm = entityManager.find(SignalRoom.class, room.getId());
        User partner = latestRoomForm.getPartnerUser(userId);
        User user = userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));

        boolean isSender = Objects.equals(userId, latestRoomForm.getSenderUser().getId());
        MatchingStatus partnerStatus = isSender ? latestRoomForm.getReceiverMatchingStatus() : latestRoomForm.getSenderMatchingStatus();

        if (partnerStatus == MatchingStatus.SIGNAL) {
            sseChannelService.notifyMatchingConfirmedToPartner(latestRoomForm, user, partner);
        } else {
            sseChannelService.notifyMatchingResultToPartner(latestRoomForm, user, partner, matchingStatus);
        }
    }

    @Async
    @Transactional
    public void createMatchingAlarm(SignalRoom room, Long userId) {
        SignalRoom latestRoomForm = entityManager.find(SignalRoom.class, room.getId());
        User partner = latestRoomForm.getPartnerUser(userId);
        User user = userRepository.findByIdWithSentSignalRooms(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()
                ));

        boolean isSender = Objects.equals(userId, latestRoomForm.getSenderUser().getId());
        MatchingStatus partnerMatchingStatus = isSender ? latestRoomForm.getReceiverMatchingStatus() : latestRoomForm.getSenderMatchingStatus();

        if (partnerMatchingStatus == MatchingStatus.SIGNAL) { return; }
        alarmService.createMatchingAlarm(latestRoomForm, user, partner);
    }
}
