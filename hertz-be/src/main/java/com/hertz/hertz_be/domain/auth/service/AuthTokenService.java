package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDTO;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.AbstractMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public Map.Entry<ReissueAccessTokenResponseDTO, String> reissueAccessToken(String refreshToken) {
        try {
            Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
            String storedToken = refreshTokenService.getRefreshToken(userId);

            if (storedToken == null || !storedToken.equals(refreshToken)) {
                throw new RefreshTokenInvalidException();
            }

            String newAccessToken = jwtTokenProvider.createAccessToken(userId);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

            long expiration = 1209600L; // 14일 (초 단위),  Todo: 공통 상수로 관리 필요
            refreshTokenService.saveRefreshToken(userId, newRefreshToken, expiration);

            return new AbstractMap.SimpleEntry<>(
                    new ReissueAccessTokenResponseDTO(newAccessToken),
                    newRefreshToken
            );
        } catch (JwtException e) {
            throw new RefreshTokenInvalidException();
        }
    }
}
