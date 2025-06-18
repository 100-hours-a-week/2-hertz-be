package com.hertz.hertz_be.domain.auth.service;

import com.hertz.hertz_be.domain.auth.client.KakaoOAuthClient;
import com.hertz.hertz_be.domain.auth.dto.request.OAuthLoginRequestDto;
import com.hertz.hertz_be.domain.auth.dto.response.OAuthLoginResult;
import com.hertz.hertz_be.domain.auth.responsecode.OAuthResponseCode;
import com.hertz.hertz_be.domain.auth.repository.OAuthRedisRepository;
import com.hertz.hertz_be.domain.auth.repository.RefreshTokenRepository;
import com.hertz.hertz_be.domain.user.entity.UserOauth;
import com.hertz.hertz_be.domain.user.repository.UserOauthRepository;
import com.hertz.hertz_be.global.auth.token.JwtTokenProvider;
import com.hertz.hertz_be.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final OAuthRedisRepository oAuthRedisRepository;
    private final StringRedisTemplate redisTemplate;
    private final UserOauthRepository userOauthRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenService;

    @Value("${oauth.kakao.client-id}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    @Value("${max.age.seconds}")
    private long maxAgeSeconds;

    public String getRedirectUrl(String provider) {
        if ("kakao".equalsIgnoreCase(provider)) {
            String state = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("OAUTH:STATE:" + state, "VALID", Duration.ofMinutes(10));

            return "https://kauth.kakao.com/oauth/authorize"
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + redirectUri
                    + "&response_type=code"
                    + "&state=" + state;
        }
        throw new BusinessException(
                OAuthResponseCode.UNSUPPORTED_PROVIDER.getCode(),
                OAuthResponseCode.UNSUPPORTED_PROVIDER.getHttpStatus(),
                OAuthResponseCode.UNSUPPORTED_PROVIDER.getMessage()
        );
    }

    public OAuthLoginResult oauthLogin(String provider, OAuthLoginRequestDto request) {
        if (!"kakao".equalsIgnoreCase(provider)) {
            throw new BusinessException(
                    OAuthResponseCode.UNSUPPORTED_PROVIDER.getCode(),
                    OAuthResponseCode.UNSUPPORTED_PROVIDER.getHttpStatus(),
                    OAuthResponseCode.UNSUPPORTED_PROVIDER.getMessage()
            );
        }

        String stateKey = "OAUTH:STATE:" + request.getState();
        if (!"VALID".equals(redisTemplate.opsForValue().get(stateKey))) {
            throw new BusinessException(
                    OAuthResponseCode.OAUTH_STATE_INVALID.getCode(),
                    OAuthResponseCode.OAUTH_STATE_INVALID.getHttpStatus(),
                    OAuthResponseCode.OAUTH_STATE_INVALID.getMessage()
            );
        }
        redisTemplate.delete(stateKey);

        // 카카오에서 사용자 정보 및 토큰 가져오기
        Map<String, Object> kakaoData;
        try {
            kakaoData = kakaoOAuthClient.getUserInfoAndTokens(request.getCode());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BusinessException(
                    OAuthResponseCode.RATE_LIMIT.getCode(),
                    OAuthResponseCode.RATE_LIMIT.getHttpStatus(),
                    OAuthResponseCode.RATE_LIMIT.getMessage()
            );
        }
        String providerId = kakaoData.get("id").toString();
        String providerRefreshToken = kakaoData.get("refresh_token").toString();
        long refreshTokenExpiresIn = Long.parseLong(kakaoData.get("refresh_token_expires_in").toString());

        // 기존 회원인지 확인
        if (isUserRegistered(providerId, provider)) {
            Long userId = getUserIdByProvider(providerId, provider);

            String accessToken = jwtTokenProvider.createAccessToken(userId);

            String refreshToken = jwtTokenProvider.createRefreshToken(userId);
            refreshTokenService.saveRefreshToken(userId, refreshToken, maxAgeSeconds);

            return OAuthLoginResult.registered(userId, accessToken, refreshToken);
        }

        oAuthRedisRepository.saveRefreshToken(providerId, providerRefreshToken, refreshTokenExpiresIn);
        return OAuthLoginResult.notRegistered(providerId);
    }

    private boolean isUserRegistered(String providerId, String provider) {
        return userOauthRepository.existsByProviderIdAndProvider(providerId, provider);
    }

    public Long getUserIdByProvider(String providerId, String provider) {
        UserOauth userOauth = userOauthRepository.findByProviderIdAndProvider(providerId, provider).orElseThrow();
        return userOauth.getUser().getId();
    }
}
