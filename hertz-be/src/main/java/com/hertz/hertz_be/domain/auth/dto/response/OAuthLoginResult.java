package com.hertz.hertz_be.domain.auth.dto.response;

public record OAuthLoginResult(
        boolean registered,
        Long userId,
        String accessToken,
        String refreshToken,
        String providerId
) {
    public static OAuthLoginResult registered(Long userId, String accessToken, String refreshToken) {
        return new OAuthLoginResult(true, userId, accessToken, refreshToken, null);
    }

    public static OAuthLoginResult notRegistered(String providerId) {
        return new OAuthLoginResult(false, null, null, null, providerId);
    }
}
