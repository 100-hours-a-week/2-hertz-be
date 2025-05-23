package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.entity.SignalMessage;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.dto.object.UserMessageCountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncChannelService {
    private final long ONE_MESSAGE = 1L;

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
                        UserMessageCountDto::userId,
                        UserMessageCountDto::messageCount
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

        if (sentTime.plusMinutes(2).isBefore(LocalDateTime.now())) { // Todo: 프론트 연동 끝나면 24시간으로 돌려놓기
            log.info("[조건 충족] receiverUser의 첫 메시지 기준 2분 경과: roomId={}", roomId); // Todo: 프론트 연동 끝나면 24시간으로 돌려놓기

            sseChannelService.notifyMatchingConvertedInChannelRoom(
                    room, userId
            );
        }
    }

}
