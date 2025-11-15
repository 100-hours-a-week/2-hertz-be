package com.hertz.hertz_be.domain.channel.service.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.channel.controller.v1.ChannelController;
import com.hertz.hertz_be.domain.channel.dto.request.v1.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.util.AESUtil;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = ChannelController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SignalPerformanceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Mock private EntityManager entityManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private AESUtil aesUtil;
    @MockBean private AsyncChannelService asyncChannelService;
    @MockBean private ChannelService channelService;

    static List<Long> durations = Collections.synchronizedList(new ArrayList<>());
    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(162L).nickname("sender").email("sender@test.com").build();
        receiver = User.builder().id(163L).nickname("receiver").email("receiver@test.com").build();

        ReflectionTestUtils.setField(channelService, "entityManager", entityManager);
    }

    @RepeatedTest(500)
    @DisplayName("Polling 기반 채팅 API 응답 속도 측정")
    void sendSignal_polling(RepetitionInfo repetitionInfo) throws Exception {
        SendSignalRequestDto dto;
        dto = new SendSignalRequestDto(receiver.getId(), "Test Message");

        long start = System.nanoTime();

        mockMvc.perform(post("/api/v1/channel-rooms/1/messages")
                        .header("X-USER-ID", String.valueOf(162))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isCreated());

        long end = System.nanoTime();

        durations.add(end - start);
    }

    @AfterAll
    static void printStatistics() {
        durations.sort(Long::compare);
        double avgMs = durations.stream().mapToLong(Long::longValue).average().orElse(0)/1_000_000.0;
        double p95Ms = durations.get((int)(durations.size()*0.95)) / 1_000_000.0;
        System.out.printf("평균: %.2fms, P95: %.2fms%n", avgMs, p95Ms);
    }

}