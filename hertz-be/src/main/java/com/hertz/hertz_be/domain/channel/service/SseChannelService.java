package com.hertz.hertz_be.domain.channel.service;

import com.hertz.hertz_be.domain.channel.dto.response.sse.MatchingConvertedResponseDto;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseChannelService {

    private final SseService sseService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void notifyMatchingConverted(
            Long channelRoomId,
            Long senderId, String senderNickname,
            Long receiverId, String receiverNickname
    ) {
        Runnable task = () -> {
            log.info("[매칭 전환 알림] 24시간 경과 → SSE 전송 시작");

            LocalDateTime matchedAt = LocalDateTime.now();

            MatchingConvertedResponseDto senderPayload = MatchingConvertedResponseDto.builder()
                    .channelRoomId(channelRoomId)
                    .matchedAt(matchedAt)
                    .partnerId(receiverId)
                    .partnerNickname(receiverNickname)
                    .build();

            MatchingConvertedResponseDto receiverPayload = MatchingConvertedResponseDto.builder()
                    .channelRoomId(channelRoomId)
                    .matchedAt(matchedAt)
                    .partnerId(senderId)
                    .partnerNickname(senderNickname)
                    .build();

            sseService.sendToClient(senderId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), senderPayload);

            sseService.sendToClient(receiverId, SseEventName.SIGNAL_MATCHING_CONVERSION.getValue(), receiverPayload);
        };

        scheduler.schedule(task, 24, TimeUnit.HOURS);

        log.info("[매칭 전환 예약] {} ↔ {} 에 대해 24시간 후 SSE 예약 완료", senderId, receiverId);
    }
}
