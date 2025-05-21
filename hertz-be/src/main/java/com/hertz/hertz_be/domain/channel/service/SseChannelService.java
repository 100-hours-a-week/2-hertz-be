package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.sse.MatchingConvertedResponseDto;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseChannelService {

    private final SseService sseService;

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
            log.info("[매칭 전환 알림] 24시간 경과 → SSE 전송 시작");

            LocalDateTime matchedAt = LocalDateTime.now();

            sendMatchingConvertedSse(senderId, receiverId, receiverNickname, channelRoomId, matchedAt);
            sendMatchingConvertedSse(receiverId, senderId, senderNickname, channelRoomId, matchedAt);

            scheduledMap.remove(channelRoomId);
        };

        ScheduledFuture<?> future = scheduler.schedule(task, 24, TimeUnit.HOURS);
        scheduledMap.put(channelRoomId, future);

        log.info("[매칭 전환 예약] {} ↔ {} 에 대해 24시간 후 SSE 예약 완료", senderId, receiverId);
    }

    public void notifyMatchingConvertedInChannelRoom(
            SignalRoom room, Long userId, LocalDateTime matchedAt
    ) {
        if (Objects.equals(userId, room.getReceiverUser().getId())) {
            sendMatchingConvertedSse(
                    room.getReceiverUser().getId(),
                    room.getSenderUser().getId(),
                    room.getSenderUser().getNickname(),
                    room.getId(),
                    matchedAt
            );
        } else {
            sendMatchingConvertedSse(
                    room.getSenderUser().getId(),
                    room.getReceiverUser().getId(),
                    room.getReceiverUser().getNickname(),
                    room.getId(),
                    matchedAt
            );
        }
    }

    private void sendMatchingConvertedSse(Long targetUserId, Long partnerId, String partnerNickname, Long roomId, LocalDateTime matchedAt) {
        MatchingConvertedResponseDto dto = MatchingConvertedResponseDto.builder()
                .channelRoomId(roomId)
                .matchedAt(matchedAt)
                .partnerId(partnerId)
                .partnerNickname(partnerNickname)
                .build();

        sseService.sendToClient(targetUserId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), dto);
        log.info("[매칭 전환 메세지] userId={}, roomId={} 전송 완료", targetUserId, roomId);
    }
}
