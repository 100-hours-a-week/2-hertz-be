package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.object.UserMessageCountDto;
import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncChannelService {
    @Value("${matching.convert.delay-minutes}")
    private long matchingConvertDelayMinutes;
    private final long ONE_MESSAGE = 1L;

    @PersistenceContext
    private EntityManager entityManager;

    private final SignalMessageRepository signalMessageRepository;
    private final SseChannelService sseChannelService;

    @Async
    public void notifyMatchingConverted(SignalRoom room) {
        if (room.getReceiverMatchingStatus() == MatchingStatus.MATCHED &&
                room.getSenderMatchingStatus() == MatchingStatus.MATCHED) {
            return;
        }

        List<UserMessageCountDto> counts = signalMessageRepository.countMessagesBySenderInRoom(room.getId());

        Map<Long, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        UserMessageCountDto::getUserId,
                        UserMessageCountDto::getMessageCount
                ));

        if (shouldNotifyMatchingConverted(room, countMap)) {
            sseChannelService.notifyMatchingConverted(
                    room.getId(),
                    room.getSenderUser().getId(), room.getSenderUser().getNickname(),
                    room.getReceiverUser().getId(), room.getReceiverUser().getNickname()
            );
        }
    }

    private boolean shouldNotifyMatchingConverted(SignalRoom room, Map<Long, Long> countMap) {
        Long receiverId = room.getReceiverUser().getId();
        Long receiverMessageCount = countMap.getOrDefault(receiverId, 0L);
        return receiverMessageCount == ONE_MESSAGE; // Todo: 나중에 v2 배포할 때, "if (receiverMessageCount >= ONE_MESSAGE)" 로 일시적으로 수정
    }

    @Async
    public void notifyMatchingConvertedInChannelRoom(SignalRoom room, Long userId) {
        if (room.getReceiverMatchingStatus() == MatchingStatus.MATCHED && room.getSenderMatchingStatus() == MatchingStatus.MATCHED) {
            return;
        }

        Long roomId = room.getId();

        Long receiverId = room.getReceiverUser().getId();

        List<SignalMessage> messages = signalMessageRepository
                .findBySignalRoomIdAndSenderUserIdOrderBySendAtAsc(roomId, receiverId);

        if (messages.isEmpty()) return;

        SignalMessage firstMessage = messages.get(0);
        LocalDateTime sentTime = firstMessage.getSendAt();

        if (sentTime.plusMinutes(matchingConvertDelayMinutes).isBefore(LocalDateTime.now())) {
            log.info("[조건 충족] receiverUser의 첫 메시지로부터 {}분 경과: roomId={}", matchingConvertDelayMinutes, roomId);
            sseChannelService.notifyMatchingConvertedInChannelRoom(room, userId);
        }
    }

    @Async
    public void updatePartnerChannelList(SignalMessage signalMessage, Long partnerId) {
        entityManager.detach(signalMessage);
        if (signalMessage.getIsRead() == false) {
            sseChannelService.updatePartnerChannelList(signalMessage, partnerId);
        }
    }
}
