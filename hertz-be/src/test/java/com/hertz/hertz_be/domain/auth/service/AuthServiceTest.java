package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
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
public class AuthServiceTest {
    private final Long testUserId = 1L;
    private final String refreshToken = "old-refresh-token";
    private final String newAccessToken = "new-access-token";
    private final String newRefreshToken = "new-refresh-token";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("토큰 재발급 RTR - 성공")
    void reissueAccessToken_success() {
        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken))
                .thenReturn(testUserId);
        when(refreshTokenService.getRefreshToken(testUserId))
                .thenReturn(refreshToken);
        when(jwtTokenProvider.createAccessToken(testUserId))
                .thenReturn(newAccessToken);
        when(jwtTokenProvider.createRefreshToken(testUserId))
                .thenReturn(newRefreshToken);

        Map.Entry<ReissueAccessTokenResponseDto, String> result = authService.reissueAccessToken(refreshToken);

        assertEquals(newAccessToken, result.getKey().getAccessToken());
        assertEquals(newRefreshToken, result.getValue());
        verify(refreshTokenService, times(1)).saveRefreshToken(eq(testUserId), eq(newRefreshToken), anyLong());
    }

    @Test
    @DisplayName("토큰 재발급 RTR - 유효하지 않은 리프레시 토큰일 경우 실패")
    void reissueAccessToken_shouldReturnRefreshTokenInvalidException() {
        String wrongRefreshToken = "wrong-refresh-token";

        when(jwtTokenProvider.getUserIdFromRefreshToken(refreshToken))
                .thenReturn(testUserId);
        when(refreshTokenService.getRefreshToken(testUserId))
                .thenReturn(wrongRefreshToken);

        assertThrows(RefreshTokenInvalidException.class, () -> {
            authService.reissueAccessToken(refreshToken);
        });

        verify(refreshTokenService, never()).saveRefreshToken(anyLong(), anyString(), anyLong());
    }



}
