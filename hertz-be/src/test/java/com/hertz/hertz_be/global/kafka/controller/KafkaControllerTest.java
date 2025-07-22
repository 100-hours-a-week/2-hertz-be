package com.hertz.hertz_be.global.kafka.controller;

import com.hertz.hertz_be.global.kafka.dto.SseEventDto;
import com.hertz.hertz_be.global.kafka.fixture.SseEventDtoFixture;
import com.hertz.hertz_be.global.kafka.servise.KafkaProducerService;
import com.hertz.hertz_be.global.sse.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KafkaControllerTest {

    @Container
    static KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.0"))
            .withReuse(true);

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka.bootstrapAddress", kafkaContainer::getBootstrapServers);
    }

    @Autowired private KafkaProducerService kafkaProducerService;
    @Autowired private SseService sseService;
    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("KafkaController - healthcheck-topic 메시지 전송 성공")
    void sendHealthcheckMessage_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/test/kafka/ping")
                        .param("message", "healthcheck-success"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    @DisplayName("KafkaProducerService - SSE 이벤트 메시지 전송 성공")
    void sendSseEvent_shouldSucceed() {
        SseEventDto event = SseEventDtoFixture.withAll(987L, "TEST_EVENT", "test-data");

        kafkaProducerService.sendSseEvent(event);
    }

    @Test
    @DisplayName("KafkaProducerService - userId가 null인 이벤트 전송 시 NPE 발생")
    void sendSseEvent_shouldFail_whenUserIdIsNull() {
        SseEventDto badEvent = new SseEventDto(null, "test-event", "some-data");

        assertThrows(NullPointerException.class, () -> {
            kafkaProducerService.sendSseEvent(badEvent);
        });
    }

    @Test
    @DisplayName("SseService - emitter가 없는 사용자에게 전송 시 false 반환")
    void sendToClient_shouldReturnFalse_whenEmitterNotFound() {
        SseEventDto event = SseEventDtoFixture.withAll(999999L, "ALERT", "no-emitter-user");

        boolean result = sseService.sendToClient(event.userId(), event.eventName(), event.data());

        assertFalse(result);
    }

    @Test
    @DisplayName("SseService - null 이벤트로 sendErrorAndComplete 호출 시 NPE 발생하지 않아야")
    void sendErrorAndComplete_shouldNotThrow_whenEmitterIsMissing() {
        assertDoesNotThrow(() -> {
            sseService.sendErrorAndComplete(999999L, "E001", "없는 유저");
        });
    }
}
