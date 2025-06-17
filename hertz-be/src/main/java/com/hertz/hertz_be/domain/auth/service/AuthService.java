package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.exception.AuthResponseCode;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.sse.SseService;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.AbstractMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;
    private final UserRepository userRepository;
    private final SseService sseService;

    public Map.Entry<ReissueAccessTokenResponseDto, String> reissueAccessToken(String refreshToken) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
            String storedToken = refreshTokenService.getRefreshToken(userId);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new BusinessException(
                        AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                        AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(),
                        AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
            }

            String newAccessToken = jwtTokenProvider.createAccessToken(userId);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

            long expiration = maxAgeSeconds; // 14일 (초 단위),  Todo: 공통 상수로 관리 필요
            refreshTokenService.saveRefreshToken(userId, newRefreshToken, expiration);

            return new AbstractMap.SimpleEntry<>(
                    new ReissueAccessTokenResponseDto(newAccessToken),
                    newRefreshToken
            );
        } catch (JwtException e) {
            throw new BusinessException(
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getCode(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getHttpStatus(),
                    AuthResponseCode.REFRESH_TOKEN_INVALID.getMessage());
        }
    }

    public void logout(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(
                    NewResponseCode.NOT_FOUND.getCode(),
                    NewResponseCode.NOT_FOUND.getHttpStatus(),
                    "요청한 사용자를 찾을 수 없습니다."
            );
        }

        refreshTokenService.deleteRefreshToken(userId);
        sseService.disconnect(userId);
    }
}
