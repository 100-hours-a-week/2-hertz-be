package com.hertz.hertz_be.domain.auth.controller;

import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.ResponseCode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
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
            .withExposedPorts(6379);


    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = User.builder()
                .ageGroup(AgeGroup.AGE_20S)
                .gender(Gender.MALE)
                .email(UUID.randomUUID() + "@email.com")
                .profileImageUrl("http://example.com/profile.png")
                .nickname("tester")
                .oneLineIntroduction("안녕하세요")
                .build();
        userRepository.save(user);

        refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효한 리프레시 토큰일 경우 재발급 성공")
    void reissueAccessToken_shouldSucceed_whenValidRefreshToken() throws Exception {
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(post("/api/v1/auth/token")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.ACCESS_TOKEN_REISSUED))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효기간 지난 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenNoRefreshToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(ResponseCode.REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효하지 않은 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenWrongRefreshToken() throws Exception {
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);

        mockMvc.perform(post("/api/v1/auth/token")
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").value(ResponseCode.REFRESH_TOKEN_INVALID));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        refreshTokenRepository.saveRefreshToken(user.getId(), refreshToken, maxAgeSeconds);
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(delete("/api/v2/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResponseCode.LOGOUT_SUCCESS));
    }

}
