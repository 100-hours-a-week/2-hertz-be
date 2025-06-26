package com.hertz.hertz_be.domain.auth.controller;

import com.corundumstudio.socketio.SocketIOServer;
import com.hertz.hertz_be.domain.auth.fixture.UserFixture;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.auth.service.AuthService;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.config.TestContainersConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerTest extends TestContainersConfig {
    @MockBean
    private SocketIOServer socketIOServer;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private AuthService authService;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    private User user;
    private String refreshToken;

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
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.ACCESS_TOKEN_REISSUED.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.ACCESS_TOKEN_REISSUED.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value(accessToken));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효기간 지난 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenNoRefreshToken() throws Exception {
        refreshTokenRepository.deleteRefreshToken(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(post("/api/v1/auth/token")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage()));
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효하지 않은 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenWrongRefreshToken() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(post("/api/v1/auth/token")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", "invalid-token")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage()));
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() throws Exception {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(delete("/api/v2/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.LOGOUT_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.LOGOUT_SUCCESS.getMessage()));
    }

    @Test
    @DisplayName("logout API - AT 헤더 누락 시 400 반환")
    void logout_shouldReturn400_whenNoAccessToken() throws Exception {
        mockMvc.perform(delete("/api/v2/auth/logout")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.UNAUTHORIZED.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.UNAUTHORIZED.getMessage()));
    }

    @Test
    @DisplayName("logout API - 잘못된 AT 헤더 시 400 반환")
    void logout_shouldReturn400_whenInvalidAccessToken() throws Exception {
        mockMvc.perform(delete("/api/v2/auth/logout")
                        .header("Authorization", "Bearer invalid-token")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.ACCESS_TOKEN_EXPIRED.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.ACCESS_TOKEN_EXPIRED.getMessage()));
    }

    @Test
    @DisplayName("login API - 사용자 ID로 AT/RT 발급 및 쿠키 설정 성공")
    void login_shouldSucceed() throws Exception {
        Long testUserId = user.getId();
        String requestBody = String.format("{\"userId\": %d}", testUserId);

        mockMvc.perform(post("/api/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    @DisplayName("login API - 잘못된 요청(userId 누락) 시 400 반환")
    void login_shouldReturn400_whenInvalidRequest() throws Exception {
        String badRequestBody = "{}";

        mockMvc.perform(post("/api/test/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deleteUserById API - 특정 사용자 삭제")
    void deleteUserById_shouldSucceed() throws Exception {
        Long userId = user.getId();

        mockMvc.perform(delete("/api/test/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.USER_DELETE_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.USER_DELETE_SUCCESS.getMessage()));

        boolean exists = userRepository.existsById(userId);
        assert !exists;
    }

    @Test
    @DisplayName("deleteUserById API - 존재하지 않는 사용자 ID일 경우 400")
    void deleteUserById_shouldReturn400_whenUserNotFound() throws Exception {
        Long invalidId = 9999L;

        mockMvc.perform(delete("/api/test/{userId}", invalidId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(UserResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(UserResponseCode.USER_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("deleteAllUsers API - 모든 사용자 및 연관 데이터 삭제")
    void deleteAllUsers_shouldSucceed() throws Exception {

        mockMvc.perform(delete("/api/test/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AuthResponseCode.USER_DELETE_SUCCESS.getCode()))
                .andExpect(jsonPath("$.message").value(AuthResponseCode.USER_DELETE_SUCCESS.getMessage()));

        long count = userRepository.count();
        assert count == 0;
    }

}
