package com.hertz.hertz_be.global.socketio;

import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.filter.SseAuthenticationFilter;
import com.hertz.hertz_be.global.auth.handler.CustomAuthenticationEntryPoint;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.config.TestSocketIoConfig;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageRequest;
import com.hertz.hertz_be.global.socketio.dto.SocketIoMessageResponse;
import com.hertz.hertz_be.global.util.SocketIoTokenUtil;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import io.socket.client.IO;
import io.socket.client.Socket;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest()
@ActiveProfiles("test")
@Import({ TestSocketIoConfig.class, NoKafkaTestConfig.class, TestSecurityMocks.class})
@MockBean(org.springframework.kafka.annotation.KafkaListenerAnnotationBeanPostProcessor.class)
@MockBean(org.springframework.kafka.core.KafkaAdmin.class)
public class SocketIoSignalPerformanceTest {

    @MockBean
    private SocketIoService socketIoService;
    @MockBean
    private SocketIoSessionManager sessionManager;
    @MockBean
    private static UserRepository userRepository;
    @MockBean
    private static SignalRoomRepository signalRoomRepository;
    @MockBean
    private SseAuthenticationFilter sseAuthenticationFilter;
    @MockBean
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockBean
    JwtTokenProvider jwtTokenProvider;
    @MockBean
    private RedissonClient redissonClient;

    @Autowired
    private SocketIoTokenUtil socketIoTokenUtil;
    @Autowired
    SocketIoController socketIoController;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    org.springframework.context.ApplicationContext ctx;
    @Autowired
    SocketIOServer socketIoServer;

    // 공통 수신 큐
    private BlockingQueue<SocketIoMessageResponse> inbox;
    // 초기화(init_user) 동기화 래치
    private CountDownLatch initLatch;
    private Socket socket;

    private static final int port = 9092;
    private static final long AWAIT_MS = 4000L;
    private static final String message = "안녕하세요. 이건 테스트 시그널 입니다.";
    private static final long senderUserId = 163L;
    private static final long roomId = 1L;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("socketio.enabled", () -> "true");
        r.add("socketio.server.hostname", () -> "localhost");
        r.add("socketio.server.port", () -> 9092);
    }


    @Test
    void checkBean() {
        var names = ctx.getBeanNamesForType(com.hertz.hertz_be.global.auth.token.JwtTokenProvider.class);
        System.out.println("JwtTokenProvider beans = " + java.util.Arrays.toString(names));
        var bean = ctx.getBean(com.hertz.hertz_be.global.auth.token.JwtTokenProvider.class);
        System.out.println("bean@" + System.identityHashCode(bean));
    }

    @BeforeEach
    @DisplayName("=== Socket 테스트 시작 ===")
    void setUp() throws Exception {
        System.out.println("# SocketIo SetUp start #");
        // 1) 수신 준비
        this.inbox = new LinkedBlockingQueue<>();
        this.initLatch = new CountDownLatch(1);

        // 2) Mock은 connect 전 수행
        String testRefreshToken = "";
        when(jwtTokenProvider.createRefreshToken(senderUserId)).thenReturn(testRefreshToken);
        when(jwtTokenProvider.getUserIdFromToken(testRefreshToken)).thenReturn(senderUserId);
        when(jwtTokenProvider.validateToken(testRefreshToken)).thenReturn(true);

        when(userRepository.existsById(senderUserId)).thenReturn(true);
        when(signalRoomRepository.findRoomIdsByUserId(senderUserId)).thenReturn(List.of(1L, 4L));

        // 3) 클라이언트 생성 + 리스너 등록 (connect 이전)
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("cookie", List.of("refreshToken=" + testRefreshToken));
        headers.put("origin", List.of("http://localhost:3000"));

        IO.Options opts = IO.Options.builder()
                .setExtraHeaders(headers)
                .build();

        String url = "http://localhost:" + port;
        this.socket = IO.socket(url, opts);

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            System.out.println("CONNECT_ERROR: " + Arrays.toString(args));
        });
        socket.on(Socket.EVENT_DISCONNECT, args -> {
            System.out.println("DISCONNECTED");
        });
        socket.on("init_user", args -> initLatch.countDown());

        socket.on("receive_message", args -> {
            JSONObject json = (JSONObject) args[0];
            Map<String, Object> map = json.toMap();
            SocketIoMessageResponse res = objectMapper.convertValue(args[0], SocketIoMessageResponse.class);
            inbox.offer(res);
        });

        // 4) 연결 → init_user 수신 대기
        socket.connect();
    }

    @AfterEach
    @DisplayName("=== Socket 연결 해제 ===")
    void disconnect() {
        if (socket != null && socket.connected()) socket.disconnect();
    }

    @Test
    @DisplayName("권한있는 방에 브로드캐스트 테스트")
    void shouldBroadcastMessage_WhenUserHasRoomAccess() throws Exception {
        System.out.println("# 권한있는 방에 브로드캐스트 테스트 #");
        AtomicLong messageIdSeq = new AtomicLong(1000);
        LocalDateTime sendAt = LocalDateTime.now();

        // 서버에서 브로드캐스트할 응답(Mock)
        SocketIoMessageResponse mocked = new SocketIoMessageResponse(roomId, senderUserId, message, sendAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), 1L);

        when(socketIoService.processAndRespond(eq(roomId), eq(senderUserId), eq(message), eq(sendAt)))
                .thenAnswer(inv -> {
                    long id = messageIdSeq.getAndIncrement(); // auto-increment 시뮬레이션
                    return new SocketIoMessageResponse(
                            roomId,
                            senderUserId,
                            message,
                            sendAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            id
                    );
                });

        // 전송 DTO
        SocketIoMessageRequest req = new SocketIoMessageRequest(roomId, message, sendAt);

        long t0 = System.nanoTime();
        socket.emit("send_message", objectMapper.convertValue(req, Map.class));

        SocketIoMessageResponse got = inbox.poll(AWAIT_MS, MILLISECONDS);
        long rttMs = NANOSECONDS.toMillis(System.nanoTime() - t0);

        assertThat(got).isNotNull();
        assertThat(rttMs).isLessThan(1000L); // 로컬 기준 임계, 필요시 조정

        verify(socketIoService).processAndRespond(roomId, senderUserId, message, sendAt);
        // room 브로드캐스트는 controller 코드상 room-<id>로 전송되므로 수신이 되면 OK
    }

    @Test
    @DisplayName("권한없는 방 접근 제한 테스트")
    void shouldRejectMessage_WhenUserHasNoRoomAccess() throws Exception {
        long rejectRoomId = 9999L; // 권한 없음
        LocalDateTime sendAt = LocalDateTime.now();

        SocketIoMessageRequest req = new SocketIoMessageRequest(rejectRoomId, message, sendAt);

        socket.emit("send_message", objectMapper.convertValue(req, Map.class));

        // 수신 없어야 함
        SocketIoMessageResponse got = inbox.poll(800, MILLISECONDS);
        assertThat(got).isNull();
    }

    @Test
    @DisplayName("QuickCheck: RTT가 임계 미만인지 빠르게 확인")
    void shouldMeetLatencyThreshold_QuickCheck() throws Exception {
        setupMockWithAutoIncrementId();
        final int K = 5;
        final long THRESHOLD_MS = 1200L;

        List<Long> rtts = sendMessagesAndMeasureRtts(K);
        long worst = Collections.max(rtts);

        assertThat(worst).isLessThan(THRESHOLD_MS);
    }

    @Test
    @DisplayName("Distribution: 50회 측정으로 p50/p95 산출")
    void shouldMeasureMessageLatency_Distribution() throws Exception {
        setupMockWithAutoIncrementId();

        final int N = 505;
        List<Long> rtts = sendMessagesAndMeasureRtts(N);

        // 초기 5건 Warm-up 제거
        List<Long> warmupRemoved = rtts.size() > 5 ? rtts.subList(5, rtts.size()) : rtts;

        long p50 = percentile(warmupRemoved, 0.50);
        long p95 = percentile(warmupRemoved, 0.95);

        // --- 손실률 계산 ---
        int successCount = (int) warmupRemoved.stream().filter(rt -> rt >= 0).count();
        int lossCount = warmupRemoved.size() - successCount;
        double lossRate = (lossCount * 100.0) / warmupRemoved.size();

        // --- 콘솔 출력 ---
        printStats(warmupRemoved, p50, p95, successCount, lossCount, lossRate);

        assertThat(p50).isLessThan(80);
        assertThat(p95).isLessThan(150);

    }


    private void setupMockWithAutoIncrementId() {
        AtomicLong idSeq = new AtomicLong(10_000);

        // roomId, senderUserId는 eq(), message는 anyString(), sendAt은 any()으로 매칭
        when(socketIoService.processAndRespond(eq(roomId), eq(senderUserId), anyString(), any()))
                .thenAnswer(inv -> {
                    long newId = idSeq.getAndIncrement();                 // AUTO_INCREMENT 시뮬
                    String msg = inv.getArgument(2, String.class);
                    LocalDateTime sendAt = LocalDateTime.now();
                    long procAt = System.currentTimeMillis();              // 서버 처리 시각 흉내

                    return new SocketIoMessageResponse(
                            roomId,
                            senderUserId,
                            msg,
                            sendAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            newId
                    );
                });
    }

    private List<Long> sendMessagesAndMeasureRtts(int count) throws Exception {
        List<Long> rtts = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            LocalDateTime sendAtEpoch = LocalDateTime.now();
            SocketIoMessageRequest req = new SocketIoMessageRequest(
                    roomId, message, sendAtEpoch
            );

            long t0 = System.nanoTime();                                   // 단조 증가 시계로 RTT 측정
            socket.emit("send_message", objectMapper.convertValue(req, Map.class));

            SocketIoMessageResponse got = inbox.poll(AWAIT_MS, MILLISECONDS);
            long rttMs = NANOSECONDS.toMillis(System.nanoTime() - t0);

            // 손실 0을 강제하고 싶다면 null이면 실패 처리
            assertThat(got).as("message #%s should be received", i).isNotNull();
            rtts.add(rttMs);
        }

        return rtts;
    }

    private long percentile(List<Long> values, double p) {
        Collections.sort(values);
        int idx = (int) Math.ceil(values.size() * p) - 1;
        idx = Math.max(0, Math.min(idx, values.size() - 1));
        return values.get(idx);
    }

    private void printStats(
            List<Long> rtts, long p50, long p95,
            int successCount, int lossCount, double lossRate
    ) {
        long min = rtts.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = rtts.stream().mapToLong(Long::longValue).max().orElse(0);
        double avg = rtts.stream().mapToLong(Long::longValue).average().orElse(0);

        System.out.println("\n==========  Socket RTT Statistics ==========");
        System.out.printf(" - Total Samples     : %d%n", rtts.size());
        System.out.printf(" - Success Count     : %d%n", successCount);
        System.out.printf(" - Loss Count        : %d%n", lossCount);
        System.out.printf(" - Loss Rate         : %.2f%%%n", lossRate);
        System.out.println("----------------------------------------------");
        System.out.printf(" - Min RTT           : %d ms%n", min);
        System.out.printf(" - Max RTT           : %d ms%n", max);
        System.out.printf(" - Avg RTT           : %.2f ms%n", avg);
        System.out.printf(" - p50               : %d ms%n", p50);
        System.out.printf(" - p95               : %d ms%n", p95);
        System.out.println("==============================================\n");
    }
}