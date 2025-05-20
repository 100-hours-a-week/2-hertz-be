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
        } else {
            List<Object[]> counts = signalMessageRepository.countMessagesBySenderInRoom(room.getId());

            if (counts.size() != 2) return;

            Long receiverId = room.getReceiverUser().getId();

            Map<Long, Long> countMap = counts.stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0],
                            row -> (Long) row[1]
                    ));

            if (countMap.getOrDefault(receiverId, 0L) == 1) {
                sseChannelService.notifyMatchingConverted(
                        room.getId(),
                        room.getSenderUser().getId(), room.getSenderUser().getNickname(),
                        room.getReceiverUser().getId(), room.getReceiverUser().getNickname()
                );
            }
        }
    }
}
