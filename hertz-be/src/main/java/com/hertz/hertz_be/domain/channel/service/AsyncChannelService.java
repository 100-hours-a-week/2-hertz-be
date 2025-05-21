package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsyncChannelService {

    private final SignalMessageRepository signalMessageRepository;
    private final SseChannelService sseChannelService;

    @Async
    public void notifyIfExactlyOneMessageEach(SignalRoom room) {
        if (room.getReceiverMatchingStatus() == MatchingStatus.SIGNAL && room.getSenderMatchingStatus() == MatchingStatus.SIGNAL) {
            return;
        }

        List<Object[]> counts = signalMessageRepository.countMessagesBySenderInRoom(room.getId());

        Map<Long, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        Long receiverId = room.getReceiverUser().getId();
        Long receiverMessageCount = countMap.getOrDefault(receiverId, 0L);

        if (receiverMessageCount == 1) { // Todo: 나중에 v2 배포할 때, "if (receiverMessageCount >= 1)" 로 일시적으로 수정
            sseChannelService.notifyMatchingConverted(
                    room.getId(),
                    room.getSenderUser().getId(), room.getSenderUser().getNickname(),
                    room.getReceiverUser().getId(), room.getReceiverUser().getNickname()
            );
        }
    }
}
