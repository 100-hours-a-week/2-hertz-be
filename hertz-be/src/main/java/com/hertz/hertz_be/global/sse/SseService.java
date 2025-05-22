package com.hertz.hertz_be.global.sse;

import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {
    private final UserRepository userRepository;
    // 무제한 유지
    private static final Long TIMEOUT = 0L;

    // userId -> emitter 매핑
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 기존 연결 제거
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        // 프론트에서 eventSource.close()를 호출하면 실행됨
        emitter.onCompletion(() -> {
            log.info("✅ SSE 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("⌛ SSE 타임아웃 발생: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        // 최초 연결 시 ping 전송
        sendToClient(userId, SseEventName.PING.getValue(), "connect success");
        log.warn("connect success: userId={}", userId);

        return emitter;
    }

    // 15초마다 heartbeat 전송 -> 헬스체크 역할
    @Scheduled(fixedRate = 15000)
    public void sendPeriodicPings() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventName.HEARTBEAT.getValue())
                        .data("heartbeat"));
                //log.warn("heartbeat: userId={}", userId);
            } catch (IOException e) {
                log.warn("⚠️ heartbeat 전송 실패: userId={}, 연결 종료", userId);
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    public void sendToClient(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null && data != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.warn("🚫 이벤트 전송 실패, 연결 종료: userId={}", userId);
                emitter.complete();
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
