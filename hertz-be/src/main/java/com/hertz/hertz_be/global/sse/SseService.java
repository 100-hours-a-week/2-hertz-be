package com.hertz.hertz_be.global.sse;

import com.hertz.hertz_be.global.common.ResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SseService {

    private static final Long TIMEOUT = 60 * 1000L; // 1분
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        // 기존 연결 제거 (덮어쓰기 방식)
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);

        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        sendToClient(userId, "ping", new ResponseDto<>(
                "PING",
                "keep-alive",
                null
        ));

        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendPeriodicPings() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ping")
                        .data(new ResponseDto<>("PING", "keep-alive", null)));
            } catch (IOException e) {
                log.warn("주기적 Ping 실패: userId={}", userId);
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    public void sendToClient(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.warn("SSE 전송 실패, emitter 제거: userId={}", userId);
                emitters.remove(userId);
            }
        }
    }

    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
        }
    }
}
