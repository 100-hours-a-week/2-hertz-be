package com.hertz.hertz_be.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuthLoginResult {

    private final boolean registered;

    // 기존 회원용
    private final Long userId;
    private final String accessToken;
    private final String refreshToken;

    // 신규 회원용
    private final String providerId;

    public static OAuthLoginResult registered(Long userId, String accessToken, String refreshToken) {
        return OAuthLoginResult.builder()
                .registered(true)
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public static OAuthLoginResult notRegistered(String providerId) {
        return OAuthLoginResult.builder()
                .registered(false)
                .providerId(providerId)
                .build();
    }
}
