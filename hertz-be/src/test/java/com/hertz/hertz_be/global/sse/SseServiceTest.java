package com.hertz.hertz_be.global.sse;

import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.common.SseEventName;
import com.hertz.hertz_be.global.kafka.exception.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SseServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private SseService sseService;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("SSE 구독 - 신규 연결 성공")
    void subscribe_newConnection_success() {
        when(userRepository.existsById(userId)).thenReturn(true);

        SseEmitter emitter = sseService.subscribe(userId);

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("SSE 구독 - 존재하지 않는 유저일 경우 에러 이벤트 전송")
    void subscribe_nonExistentUser_shouldSendError() {
        when(userRepository.existsById(userId)).thenReturn(false);

        SseEmitter emitter = sseService.subscribe(userId);

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("SSE 구독 - 이미 연결된 유저가 재구독 시도 시 connect success 재전송")
    void subscribe_existingEmitter_shouldSendConnectSuccessAgain() throws Exception {
        when(userRepository.existsById(userId)).thenReturn(true);

        SseEmitter emitter = spy(new SseEmitter(1800000L));
        // 직접 emitters 맵에 삽입
        getEmitterMap(sseService).put(userId, emitter);

        SseEmitter returned = sseService.subscribe(userId);

        verify(emitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
        assertEquals(emitter, returned);
    }

    @Test
    @DisplayName("sendToClient - 정상 전송")
    void sendToClient_success() throws Exception {
        String eventName = "test-event";
        String data = "hello";

        when(refreshTokenRepository.getRefreshToken(userId)).thenReturn("valid-token");

        SseEmitter emitter = spy(new SseEmitter(1800000L));
        getEmitterMap(sseService).put(userId, emitter);

        boolean result = sseService.sendToClient(userId, eventName, data);

        assertTrue(result);
        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    @DisplayName("sendToClient - 리프레시 토큰 없음")
    void sendToClient_shouldReturnFalse_whenNoRefreshToken() {
        when(refreshTokenRepository.getRefreshToken(userId)).thenReturn(null);

        boolean result = sseService.sendToClient(userId, "event", "data");

        assertFalse(result);
    }

    @Test
    @DisplayName("sendToClient - emitter 없을 경우 false 반환")
    void sendToClient_shouldReturnFalse_whenEmitterMissing() {
        when(refreshTokenRepository.getRefreshToken(userId)).thenReturn("token");

        boolean result = sseService.sendToClient(userId, "event", "data");

        assertFalse(result);
    }

    @Test
    @DisplayName("sendToClient - IOException 발생 시 KafkaException 전환")
    void sendToClient_shouldThrow_whenIOException() throws Exception {
        when(refreshTokenRepository.getRefreshToken(userId)).thenReturn("token");
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(IOException.class).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        // emitter를 직접 맵에 주입
        getEmitterMap(sseService).put(userId, emitter);

        KafkaException exception = assertThrows(KafkaException.class, () -> {
            sseService.sendToClient(userId, "test", "data");
        });

        assertTrue(exception.getMessage().contains("IOException"));
        verify(emitter).complete(); // emitter 정리 확인
    }

    @Test
    @DisplayName("sendErrorAndComplete - 정상 처리")
    void sendErrorAndComplete_success() throws IOException {
        SseEmitter emitter = spy(new SseEmitter(1800000L));
        getEmitterMap(sseService).put(userId, emitter);

        sseService.sendErrorAndComplete(userId, "404", "Not Found");

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
        assertFalse(getEmitterMap(sseService).containsKey(userId));
    }

    @Test
    @DisplayName("disconnect - 정상 종료")
    void disconnect_shouldRemoveEmitter() {
        SseEmitter emitter = sseService.subscribe(userId);

        sseService.disconnect(userId);

        // emitters에서 제거되었는지 확인
        assertFalse(getEmitterMap(sseService).containsKey(userId));
    }

    @Test
    @DisplayName("sendPeriodicPings - emitter가 있을 때 heartbeat 전송")
    void sendPeriodicPings_shouldSendHeartbeat() {
        SseEmitter emitter = spy(new SseEmitter(1800000L));
        getEmitterMap(sseService).put(userId, emitter);

        sseService.sendPeriodicPings();

        try {
            verify(emitter, atLeastOnce()).send(any(SseEmitter.SseEventBuilder.class));
        } catch (IOException e) {
            fail("IOException should not be thrown");
        }
    }

    // SseService의 private 필드인 emitters에 접근하기 위한 리플렉션 헬퍼 메서드
    @SuppressWarnings("unchecked")
    private Map<Long, SseEmitter> getEmitterMap(SseService service) {
        try {
            var field = SseService.class.getDeclaredField("emitters");
            field.setAccessible(true);
            return (Map<Long, SseEmitter>) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
