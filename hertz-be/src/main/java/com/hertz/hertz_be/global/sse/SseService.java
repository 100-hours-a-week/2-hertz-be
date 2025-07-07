package com.hertz.hertz_be.global.sse;

import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.kafka.exception.KafkaException;
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

    private static final Long TIMEOUT = 1_800_000L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        if (!userRepository.existsById(userId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of(
                                "code", NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                                "message", String.format("SSE 연결 요청한 사용자가(userId=%s) 존재하지 않습니다.", userId)
                        )));
            } catch (IOException ignored) {
                // 전송 실패해도 무시
            } finally {
                emitter.complete();
            }
            return emitter;
        }

        // 이미 연결되어 있으면 기존 emitter 종료
        if (emitters.containsKey(userId)) {
            emitters.get(userId).complete();
            emitters.remove(userId);
        }

        emitters.put(userId, emitter);

        // 연결 정상 종료
        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료: userId={}", userId);
            emitters.remove(userId);
        });

        // 연결 타임아웃
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃: userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
        });

        // 오류 발생
        emitter.onError(throwable -> {
            if (throwable instanceof org.apache.catalina.connector.ClientAbortException) {
                log.warn("SSE ClientAbortException: 클라이언트 강제 종료 userId={}", userId);
            } else if (throwable instanceof IOException) {
                log.warn("SSE IOException 발생: userId={}, message={}", userId, throwable.getMessage());
            } else {
                log.error("SSE 기타 오류 발생: userId={}, error={}", userId, throwable.toString());
            }
            emitter.complete();
            emitters.remove(userId);
        });

        // 최초 연결 성공 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name(SseEventName.PING.getValue())
                    .data("connect success"));
            log.info("SSE 연결 성공: userId={}", userId);
        } catch (IllegalStateException | IOException e) {
            log.warn("connect success 전송 실패: userId={}, 사유: {}", userId, e.getMessage());
            emitter.complete();
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * 주기적 heartbeat 전송 (15초마다)
     */
    @Scheduled(fixedRate = 15000)
    public void sendPeriodicPings() {
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventName.HEARTBEAT.getValue())
                        .data("heartbeat"));
            } catch (IllegalStateException | IOException e) {
                log.warn("heartbeat 전송 실패: userId={}, 사유: {}", userId, e.getMessage());
                emitter.complete();
                emitters.remove(userId);
            }
        });
    }

    /**
     * 개별 사용자에게 이벤트 전송
     */
    public boolean sendToClient(Long userId, String eventName, Object data) {
        String storedToken = refreshTokenService.getRefreshToken(userId);
        if (storedToken == null) {
            sendErrorAndComplete(userId,
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
            return false;
        }

        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IllegalStateException e) {
            log.warn("SSE 이벤트 전송 시 IllegalStateException: 이미 완료된 emitter에 send 시도 userId={}", userId);
            emitter.complete();
            emitters.remove(userId);
            throw new KafkaException("SSE 전송 실패: 이미 완료된 emitter", e);
        } catch (IOException e) {
            log.warn("SSE 이벤트 전송 시 IOException: 이벤트 전송 실패 userId={}, message={}", userId, e.getMessage());
            emitter.complete();
            emitters.remove(userId);
            throw new KafkaException("SSE 전송 실패: IOException", e);
        }
    }

    /**
     * 오류 이벤트 전송 후 연결 종료
     */
    public void sendErrorAndComplete(Long userId, String code, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data(Map.of("code", code, "message", message)));
            } catch (IOException | IllegalStateException e) {
                log.warn("오류 이벤트 전송 실패: userId={}, 사유: {}", userId, e.getMessage());
            } finally {
                emitter.complete();
                emitters.remove(userId);
            }
        }
    }

    /**
     * 강제 연결 종료
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(userId);
            log.info("로그아웃을 통한 강제 연결 종료: userId={}", userId);
        }
    }
}
