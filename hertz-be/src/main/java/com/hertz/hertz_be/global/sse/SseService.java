package com.hertz.hertz_be.global.sse;

import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.SseEventName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenService;

    private static final Long TIMEOUT = 0L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }

        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃 발생: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        sendToClient(userId, SseEventName.PING.getValue(), "connect success");
        log.warn("connect success: userId={}", userId);

        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendPeriodicPings() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventName.HEARTBEAT.getValue())
                        .data("heartbeat"));
            } catch (IOException e) {
                log.warn("heartbeat 전송 실패: userId={}, 연결 종료", userId);
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    public boolean sendToClient(Long userId, String eventName, Object data) {
        String storedToken = refreshTokenService.getRefreshToken(userId);
        if (storedToken == null) {
            sendErrorAndComplete(userId, ResponseCode.REFRESH_TOKEN_INVALID, "Refresh Token이 유효하지 않거나 만료되었습니다. 다시 로그인 해주세요.");
            return false;
        }

        SseEmitter emitter = emitters.get(userId);
        if (emitter != null && data != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                return true;
            } catch (IOException e) {
                log.warn("이벤트 전송 실패, 연결 종료: userId={}", userId);
                emitter.complete();
                emitters.remove(userId);
                return false;
            }
        }
        return false;
    }

    public void sendErrorAndComplete(Long userId, String code, String message) {
        SseEmitter emitter = emitters.get(userId);
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(Map.of("code", code, "message", message)));
            emitter.complete();
            emitters.remove(userId);
        } catch (IOException ex) {
            emitter.complete();
            emitters.remove(userId);
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
