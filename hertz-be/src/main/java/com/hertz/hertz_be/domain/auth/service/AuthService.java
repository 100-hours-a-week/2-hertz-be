package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.dto.response.ReissueAccessTokenResponseDto;
import com.hertz.hertz_be.domain.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.channel.exception.UserNotFoundException;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
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

    public void logout(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        refreshTokenService.deleteRefreshToken(userId);
        sseService.disconnect(userId);
    }
}
