package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.AbstractMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;

    public Map.Entry<ReissueAccessTokenResponseDto, String> reissueAccessToken(String refreshToken) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
            String storedToken = refreshTokenService.getRefreshToken(userId);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new RefreshTokenInvalidException();
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
            throw new RefreshTokenInvalidException();
        }
    }
}
