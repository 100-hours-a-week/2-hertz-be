package com.hertz.hertz_be.domain.channel.service.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.channel.controller.v1.ChannelController;
import com.hertz.hertz_be.domain.channel.dto.request.v1.SendSignalRequestDto;
import com.hertz.hertz_be.domain.channel.service.AsyncChannelService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.util.AESUtil;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(controllers = {ChannelController.class, com.hertz.hertz_be.domain.channel.controller.v3.ChannelController.class})
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
    @MockBean private com.hertz.hertz_be.domain.channel.service.v3.ChannelService channelService_v3;

    static List<Long> durations = Collections.synchronizedList(new ArrayList<>());
    static final List<Long> pollingLatencies = new ArrayList<>(); // E2E용

    static final long POLL_INTERVAL_MS = 3000L;   // Polling 주기 (3초 가정)
    static final int MESSAGE_COUNT = 100;          // 실험에 사용할 메시지 개수

    private User sender;
    private User receiver;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(162L).nickname("sender").email("sender@test.com").build();
        receiver = User.builder().id(163L).nickname("receiver").email("receiver@test.com").build();

        ReflectionTestUtils.setField(channelService, "entityManager", entityManager);
    }

    @RepeatedTest(10)
    @DisplayName("Polling 기반 채팅 API 응답 속도 측정")
    void sendSignal_polling(RepetitionInfo repetitionInfo) throws Exception {
        SendSignalRequestDto dto;
        dto = new SendSignalRequestDto(receiver.getId(), "Test Message");

        long start = System.nanoTime();

        mockMvc.perform(post("/api/v1/channel-rooms/1/messages")
                        .header("X-USER-ID", String.valueOf(sender.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isCreated());

        long end = System.nanoTime();

        durations.add(end - start);
    }

    @Test
    @DisplayName("Polling 구조에서 메시지 생성~수신까지 E2E 지연 측정")
    void polling_endToEndLatency() throws Exception {
        // 메시지별 createdAt 저장용 (id -> createdAt)
        Map<Integer, Long> createdAtMap = new HashMap<>();
        // 메시지별 최초 수신 여부
        Set<Long> received = new HashSet<>();

        long lastSeenMessageId = 0L;

        // 1) 먼저 MESSAGE_COUNT 만큼 메시지 생성 (send API 호출)
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            SendSignalRequestDto dto =
                    new SendSignalRequestDto(receiver.getId(), "E2E Test " + i);

            long createdAt = System.currentTimeMillis();

            mockMvc.perform(post("/api/v1/channel-rooms/1/messages")
                            .header("X-USER-ID", String.valueOf(sender.getId()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andDo(print())
                    .andExpect(status().isCreated());

            // messageId 파싱
            // String json = result.getResponse().getContentAsString();
            //long messageId = JsonPath.read(json, "$.id");

            createdAtMap.put(i++, createdAt);
        }

        // 2) Polling 루프: 새 메시지를 polling으로 처음 발견할 때까지 반복
        while (received.size() < MESSAGE_COUNT) {
            Thread.sleep(POLL_INTERVAL_MS);   // Polling 주기

            long pollTime = System.currentTimeMillis();

            MvcResult pollResult = mockMvc.perform(get("/api/v3/channel-rooms/1")
                            .header("X-USER-ID", String.valueOf(receiver.getId()))
                            .param("lastMessageId", String.valueOf(lastSeenMessageId)))
                    .andDo(print())   // 1) 요청/응답 + 예외 로그 콘솔 출력
                    .andReturn();     // 2) 결과 객체 받기

            Exception ex = pollResult.getResolvedException();
            if (ex != null) {
                ex.printStackTrace();   // 3) 실제 예외 스택 트레이스 출력
            }

            String body = pollResult.getResponse().getContentAsString();

            // ★ 응답이 [ { "id": 1, ... }, { "id": 2, ... } ] 이런 배열이라고 가정
            //   네 구조에 맞게 JsonPath / ObjectMapper로 파싱하면 됨
            List<Integer> polledIds = JsonPath.read(body, "$.[*].id");

            for (Integer polledId : polledIds) {
                long id = polledId.longValue();
                lastSeenMessageId = Math.max(lastSeenMessageId, id);

                if (!received.contains(polledId) && createdAtMap.containsKey(polledId)) {
                    long createdAt = createdAtMap.get(polledId);
                    long latency = pollTime - createdAt;   // 핵심: 생성~최초 Polling 수신까지

                    pollingLatencies.add(latency);
                    received.add(id);
                }
            }
        }

        // 3) 통계 출력 (p50 / p95)
        printPollingE2EStats();
    }


    @AfterAll
    static void printAllStats() {
        // 기존 durations (쓰기 API RTT)
        // + 새로운 pollingLatencies (E2E latency) 같이 찍어도 됨
        // printStatistics();       // 기존 API RTT 통계
        printPollingE2EStats();  // 새로 추가한 Polling E2E 통계
    }


    static void printStatistics() {
        durations.sort(Long::compare);
        double avgMs = durations.stream().mapToLong(Long::longValue).average().orElse(0)/1_000_000.0;
        double p95Ms = durations.get((int)(durations.size()*0.95)) / 1_000_000.0;
        System.out.printf("평균: %.2fms, P95: %.2fms%n", avgMs, p95Ms);
    }

    static void printPollingE2EStats() {
        if (pollingLatencies.isEmpty()) {
            System.out.println("No polling E2E latencies recorded.");
            return;
        }

        pollingLatencies.sort(Long::compare);

        long min = pollingLatencies.get(0);
        long max = pollingLatencies.get(pollingLatencies.size() - 1);
        double avg = pollingLatencies.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long p50 = pollingLatencies.get((int) (pollingLatencies.size() * 0.5));
        long p95 = pollingLatencies.get((int) (pollingLatencies.size() * 0.95));

        System.out.println("\n========== 📡 Polling E2E Latency ==========");
        System.out.printf(" - Messages        : %d%n", pollingLatencies.size());
        System.out.printf(" - Poll Interval   : %d ms%n", POLL_INTERVAL_MS);
        System.out.println("-------------------------------------------");
        System.out.printf(" - Min Latency     : %d ms%n", min);
        System.out.printf(" - Max Latency     : %d ms%n", max);
        System.out.printf(" - Avg Latency     : %.2f ms%n", avg);
        System.out.printf(" - p50             : %d ms%n", p50);
        System.out.printf(" - p95             : %d ms%n", p95);
        System.out.println("===========================================\n");
    }


}