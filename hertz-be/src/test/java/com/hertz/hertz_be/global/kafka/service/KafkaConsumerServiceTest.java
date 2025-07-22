package com.hertz.hertz_be.global.kafka.service;

import com.hertz.hertz_be.global.kafka.dto.SseEventDto;
import com.hertz.hertz_be.global.kafka.exception.KafkaException;
import com.hertz.hertz_be.global.kafka.servise.KafkaConsumerService;
import com.hertz.hertz_be.global.sse.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.kafka.support.Acknowledgment;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.hertz.hertz_be.global.kafka.fixture.SseEventDtoFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    @Mock
    private SseService sseService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaConsumerService kafkaConsumerService;

    @Test
    @DisplayName("Kafka → SSE 전송 성공 시 ack 호출됨")
    void consumeToSse_success() {
        // given
        SseEventDto event = withAll(1L, "chat", "Hello, Kafka!");
        when(sseService.sendToClient(event.userId(), event.eventName(), event.data()))
                .thenReturn(true);

        // when
        kafkaConsumerService.consumeToSse(event, acknowledgment);

        // then
        verify(sseService).sendToClient(event.userId(), event.eventName(), event.data());
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("Kafka → SSE 전송 중 예외 발생 시 KafkaException throw")
    void consumeToSse_shouldThrowKafkaException_whenSseFails() {
        // given
        SseEventDto event = withAll(1L, "chat", "This will fail");
        when(sseService.sendToClient(event.userId(), event.eventName(), event.data()))
                .thenThrow(new RuntimeException("SSE 실패"));

        // when & then
        KafkaException exception = assertThrows(KafkaException.class, () -> {
            kafkaConsumerService.consumeToSse(event, acknowledgment);
        });

        assertTrue(exception.getMessage().contains("Kafka → SSE 처리 중 알 수 없는 예외 발생"));
        verify(sseService).sendToClient(event.userId(), event.eventName(), event.data());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    @DisplayName("DLQ 메시지 소비 - 로그 출력 (예외 없음)")
    void consumeDlq_logsFailedEvent() {
        // given
        SseEventDto failedEvent = defaultEvent();

        // when
        kafkaConsumerService.consumeDlq(failedEvent);

        // then - verify 대신 예외 없이 수행되는지 확인
    }

    @Test
    @DisplayName("Healthcheck 메시지 소비 - 로그 출력")
    void consumeHealthcheck_logsMessage() {
        kafkaConsumerService.consumeHealthcheck("Kafka consumer is alive");
    }
}
