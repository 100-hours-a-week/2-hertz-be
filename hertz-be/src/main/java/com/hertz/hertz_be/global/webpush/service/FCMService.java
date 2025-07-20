package com.hertz.hertz_be.global.webpush.service;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.webpush.responsecode.FCMEventType;
import com.hertz.hertz_be.global.webpush.responsecode.FCMResponseCode;
import com.hertz.hertz_be.global.webpush.token.FCMTokenDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    private final FCMTokenDao fcmTokenDao;
    private final UserRepository userRepository;
    private final Map<FCMEventType, Set<Long>> notifiedMatchingRooms = new ConcurrentHashMap<>();

    public void saveToken(Long userId, String token) {
        try {
            User user = getActiveUserOrThrow(userId);
            fcmTokenDao.saveToken(userId, token);
        } catch (Exception e) {
            throw new BusinessException(
                    FCMResponseCode.FCM_TOKEN_SAVED_FAIL.getCode(),
                    FCMResponseCode.FCM_TOKEN_SAVED_FAIL.getHttpStatus(),
                    null
            );
        }
    }

    public void sendWebPush(Long firstUserId, Long secondUserId, String title, String content) {
        sendWebPush(firstUserId, title, content);
        sendWebPush(secondUserId, title, content);
    }

    // 사용자에게 push 알림
    @Transactional
    public void sendWebPush(Long userId, String title, String content){
        if (!fcmTokenDao.hasKey(userId)) {
            log.warn("❌ FCM 토큰 없음: userId = {}", userId);
            return;
        }

        String token = fcmTokenDao.getToken(userId);

        Message message = Message.builder()
                .setToken(token)
                .setWebpushConfig(WebpushConfig.builder()
                        .putHeader("ttl", "300")
                        .putData("title", title)
                        .putData("content", content)
                        .build())
                .build();
        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        ApiFuture<String> future = firebaseMessaging.sendAsync(message);

        ApiFutures.addCallback(future, new ApiFutureCallback<>() {
            @Override
            public void onSuccess(String result) {
                log.info("✅ FCM 전송 성공: {}", result);
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("❌ FCM 전송 실패: {}", t.getMessage(), t);
            }
        }, MoreExecutors.directExecutor());
    }

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()));
    }

    public boolean shouldNotify(FCMEventType eventType, Long channelRoomId) {
        notifiedMatchingRooms.computeIfAbsent(eventType, k -> ConcurrentHashMap.newKeySet());
        return notifiedMatchingRooms.get(eventType).add(channelRoomId);
    }

}