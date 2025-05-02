package com.hertz.hertz_be.domain.auth.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Repository
@RequiredArgsConstructor
public class OAuthRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIX = "KAKAO:RT:";

    public void saveRefreshToken(String providerId, String refreshToken, long expiresInSeconds) {
        String key = PREFIX + providerId;
        Instant expiryInstant = Instant.now().plusSeconds(expiresInSeconds);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));
        String formattedExpiry = formatter.format(expiryInstant);

        String value = refreshToken + "," + formattedExpiry;
        redisTemplate.opsForValue().set(key, value, Duration.ofDays(2));
    }

    public String get(String providerId) { //Todo: 메서드 네임 리팩토링 필요
        return redisTemplate.opsForValue().get(PREFIX + providerId);
    }

    public void delete(String providerId) { //Todo: 메서드 네임 리팩토링 필요
        redisTemplate.delete(PREFIX + providerId);
    }
}