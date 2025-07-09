package com.hertz.hertz_be.global.webpush.token;


import com.hertz.hertz_be.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FCMTokenDao {

    private final StringRedisTemplate tokenRedisTemplate;

    public void saveToken(Long userId, String token) {
        tokenRedisTemplate.opsForValue()
                .set("fcm-token-userId-" + String.valueOf(userId), token);
    }

    public String getToken(Long userId) {
        return tokenRedisTemplate.opsForValue().get(userId);
    }

    public void deleteToken(Long userId) {
        tokenRedisTemplate.delete(String.valueOf(userId));
    }

    public boolean hasKey(Long userId) {
        return tokenRedisTemplate.hasKey(String.valueOf(userId));
    }
}