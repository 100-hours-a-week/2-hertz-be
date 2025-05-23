package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.sse.MatchingConvertedInChannelRoomResponseDTO;
import com.hertz.hertz_be.domain.channel.dto.response.sse.MatchingConvertedResponseDto;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.MatchingStatus;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseChannelService {
    @Value("${matching.convert.delay-minutes}")
    private long matchingConvertDelayMinutes;

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
            log.info("[매칭 전환 알림] {}분 경과 → SSE 전송 시작",matchingConvertDelayMinutes);

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
        Long targetUserId;
        MatchingStatus status;

        if (Objects.equals(userId, room.getReceiverUser().getId())) {
            targetUserId = room.getReceiverUser().getId();
            status = room.getReceiverMatchingStatus();
        } else {
            targetUserId = room.getSenderUser().getId();
            status = room.getSenderMatchingStatus();
        }

        if (status == MatchingStatus.MATCHED) {
            sendMatchingConvertedInChannelRoom(targetUserId, room.getId(), true);
        }
        sendMatchingConvertedInChannelRoom(targetUserId, room.getId(), false);
    }

    private void sendMatchingConvertedSse(Long targetUserId, Long partnerId, String partnerNickname, Long roomId, LocalDateTime matchedAt) {
        MatchingConvertedResponseDto dto = MatchingConvertedResponseDto.builder()
                .channelRoomId(roomId)
                .matchedAt(matchedAt)
                .partnerId(partnerId)
                .partnerNickname(partnerNickname)
                .build();

        sseService.sendToClient(targetUserId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), dto);
        log.info("[페이지 상관 없이 매칭 전환 여부 메세지] userId={}, roomId={} 전송 완료", targetUserId, roomId);
    }

    private void sendMatchingConvertedInChannelRoom(Long targetUserId, Long roomId, boolean hasResponded) {
        MatchingConvertedInChannelRoomResponseDTO dto = MatchingConvertedInChannelRoomResponseDTO.builder()
                .channelRoomId(roomId)
                .hasResponded(hasResponded)
                .build();

        sseService.sendToClient(targetUserId, SseEventName.SIGNAL_MATCHING_CONVERSION_IN_ROOM.getValue(), dto);
        log.info("[채팅방 안에서 매칭 전환 여부 메세지] userId={}, roomId={} 전송 완료", targetUserId, roomId);
    }
}
