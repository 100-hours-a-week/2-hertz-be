package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.fixture.UserFixture;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AuthService authService;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    private User user;
    private String refreshToken;

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.32")
            .withDatabaseName("testdb")
            .withUsername("testUser")
            .withPassword("testPW");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:6.2")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @BeforeEach
    void initializeUserAndRefreshToken() {
        userRepository.deleteAll();
        refreshTokenRepository.deleteAll();

        user = UserFixture.createTestUser();
        userRepository.save(user);

        refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효한 리프레시 토큰일 경우 재발급 성공")
    void reissueAccessToken_shouldSucceed_whenValidRefreshToken() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(post("/api/v1/auth/token")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_TOKEN_REISSUED))
                .andExpect(jsonPath("$.message").value("Access Token이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효기간 지난 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenNoRefreshToken() throws Exception {
        refreshTokenRepository.deleteRefreshToken(user.getId());

        mockMvc.perform(post("/api/v1/auth/token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.REFRESH_TOKEN_INVALID))
                .andExpect(jsonPath("$.message").value("Refresh Token이 유효하지 않거나 만료되었습니다. 다시 로그인 해주세요."));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효하지 않은 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenWrongRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.REFRESH_TOKEN_INVALID))
                .andExpect(jsonPath("$.message").value("Refresh Token이 유효하지 않거나 만료되었습니다. 다시 로그인 해주세요."));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(delete("/api/v2/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.LOGOUT_SUCCESS))
                .andExpect(jsonPath("$.message").value("정상적으로 로그아웃되었습니다."));
    }

    @Test
    @DisplayName("login API - 사용자 ID로 AT/RT 발급 및 쿠키 설정")
    void login_shouldSucceed() throws Exception {
        Long testUserId = user.getId();
        String requestBody = String.format("{\"userId\": %d}", testUserId);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("login API - 잘못된 요청(userId 누락) 시 400 반환")
    void login_shouldReturn400_whenInvalidRequest() throws Exception {
        String badRequestBody = "{}";

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deleteUserById API - 특정 사용자 삭제")
    void deleteUserById_shouldSucceed() throws Exception {
        Long userId = user.getId();

        mockMvc.perform(delete("/api/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_DELETE_SUCCESS))
                .andExpect(jsonPath("$.message").value("사용자가 정상적으로 삭제되었습니다."));

        boolean exists = userRepository.existsById(userId);
        assert !exists;
    }

    @Test
    @DisplayName("deleteUserById API - 존재하지 않는 사용자 ID일 경우 400")
    void deleteUserById_shouldReturn400_whenUserNotFound() throws Exception {
        Long invalidId = 9999L;

        mockMvc.perform(delete("/api/{userId}", invalidId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND))
                .andExpect(jsonPath("$.message").value("사용자가 존재하지 않습니다."));
    }

    @Test
    @DisplayName("deleteAllUsers API - 모든 사용자 및 연관 데이터 삭제")
    void deleteAllUsers_shouldSucceed() throws Exception {

        mockMvc.perform(delete("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_DELETE_SUCCESS))
                .andExpect(jsonPath("$.message").value("모든 사용자와 사용자 관련 데이터 모두 정상적으로 삭제되었습니다."));

        long count = userRepository.count();
        assert count == 0;
    }

}
