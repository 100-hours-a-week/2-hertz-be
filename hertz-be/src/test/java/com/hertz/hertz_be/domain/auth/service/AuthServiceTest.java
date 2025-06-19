package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.auth.responsecode.AuthResponseCode;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.sse.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final Long testUserId = 1L;
    private final String refreshToken = "old-refresh-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SseService sseService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("토큰 재발급 RTR - 성공")
    void reissueAccessToken_success() {
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken))
                .thenReturn(testUserId);
        when(refreshTokenService.getRefreshToken(testUserId))
                .thenReturn(refreshToken);
        when(jwtTokenProvider.createAccessToken(testUserId))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.createRefreshToken(testUserId))
                .thenReturn(newRefreshToken);

        Map.Entry<ReissueAccessTokenResponseDto, String> result = authService.reissueAccessToken(refreshToken);

        assertEquals(newAccessToken, result.getKey().accessToken());
        assertEquals(newRefreshToken, result.getValue());

        verify(jwtTokenProvider, times(1)).getUserIdFromRefreshToken(refreshToken);
        verify(jwtTokenProvider, times(1)).createAccessToken(testUserId);
        verify(jwtTokenProvider, times(1)).createRefreshToken(testUserId);
        verify(refreshTokenService, times(1)).saveRefreshToken(eq(testUserId), eq(newRefreshToken), anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효하지 않은 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowBusinessException_whenWrongRefreshToken() {
        String wrongRefreshToken = "wrong-refresh-token";

        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken))
                .thenReturn(testUserId);
        when(refreshTokenService.getRefreshToken(testUserId))
                .thenReturn(wrongRefreshToken);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.reissueAccessToken(refreshToken);
        });

        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(), exception.getStatus());
        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage(), exception.getMessage());

        verify(refreshTokenService, never()).saveRefreshToken(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효기간 지난 리프레시 토큰일 경우 예외 발생")
    void reissueAccessToken_shouldThrowRefreshTokenInvalidException_whenExpiredRefreshToken() {
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken))
                .thenReturn(testUserId);
        when(refreshTokenService.getRefreshToken(testUserId))
                .thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.reissueAccessToken(refreshToken);
        });

        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(), exception.getStatus());
        assertEquals(AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage(), exception.getMessage());

        verify(refreshTokenService, never()).saveRefreshToken(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("로그아웃 - 성공")
    void logout_success() {
        when(userRepository.existsById(testUserId)).thenReturn(true);

        authService.logout(testUserId);

        verify(userRepository, times(1)).existsById(testUserId);
        verify(refreshTokenService, times(1)).deleteRefreshToken(testUserId);
        verify(sseService, times(1)).disconnect(testUserId);
    }

    @Test
    @DisplayName("로그아웃 - 존재하지 않는 유저일 경우 예외 발생")
    void logout_shouldThrowUserNotFoundException() {
        when(userRepository.existsById(testUserId)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            authService.logout(testUserId);
        });

        assertEquals(UserResponseCode.USER_NOT_FOUND.getCode(), exception.getCode());
        assertEquals(UserResponseCode.USER_NOT_FOUND.getHttpStatus(), exception.getStatus());
        assertEquals("로그아웃을 요청한 사용자가 존재하지 않습니다.", exception.getMessage());

        verify(userRepository, times(1)).existsById(testUserId);
        verify(refreshTokenService, never()).deleteRefreshToken(anyLong());
        verify(sseService, never()).disconnect(anyLong());
    }
}
