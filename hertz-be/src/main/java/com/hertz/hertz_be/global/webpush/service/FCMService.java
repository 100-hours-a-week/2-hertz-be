package com.hertz.hertz_be.global.webpush.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.repository.UserRepository;
import com.hertz.hertz_be.domain.user.responsecode.UserResponseCode;
import com.hertz.hertz_be.global.exception.BusinessException;
import com.hertz.hertz_be.global.webpush.responsecode.FCMResponseCode;
import com.hertz.hertz_be.global.webpush.token.FCMTokenDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    private final FCMTokenDao fcmTokenDao;
    private final UserRepository userRepository;

    public void saveToken(Long userId, String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BusinessException(
                    FCMResponseCode.FCM_INVALID_TOKEN.getCode(),
                    FCMResponseCode.FCM_INVALID_TOKEN.getHttpStatus(),
                    FCMResponseCode.FCM_INVALID_TOKEN.getMessage()
            );
        }

        try {
            User user = getActiveUserOrThrow(userId);
            fcmTokenDao.saveToken(userId, token);
        } catch (Exception e) {
            log.error(":: FCM Token 저장 중 예외 발생 ::", e);
            throw new BusinessException(
                    FCMResponseCode.FCM_TOKEN_SAVED_FAIL.getCode(),
                    FCMResponseCode.FCM_TOKEN_SAVED_FAIL.getHttpStatus(),
                    FCMResponseCode.FCM_TOKEN_SAVED_FAIL.getMessage()
            );
        }
    }

    // 사용자에게 push 알림
    @Transactional(readOnly = true)
    public ResponseEntity<?> pushNotification(Long userId, String title, String content){
        Map<String, Object> resultMap = new HashMap<>();
        HttpStatus status = null;
        try {
            User user = getActiveUserOrThrow(userId);
            if (!fcmTokenDao.hasKey(userId)) {
                resultMap.put("message", "유저의 FireBase 토큰이 없습니다.");
                status = HttpStatus.BAD_REQUEST;
            }
            else {
                String token = fcmTokenDao.getToken(userId);
                Message message = Message.builder()
                        .setToken(token)
                        .setWebpushConfig(WebpushConfig.builder()
                                .putHeader("ttl", "300")
                                .setNotification(new WebpushNotification(title, content))
                                .build())
                        .build();
                String response = FirebaseMessaging.getInstance().sendAsync(message).get();
                status = HttpStatus.OK;
                resultMap.put("response", response);
            }
        } catch (Exception e) {
            resultMap.put("message", "요청 실패");
            resultMap.put("exception", e.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(resultMap, status);
    }

    private User getActiveUserOrThrow(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(
                        UserResponseCode.USER_NOT_FOUND.getCode(),
                        UserResponseCode.USER_NOT_FOUND.getHttpStatus(),
                        UserResponseCode.USER_NOT_FOUND.getMessage()));
    }

}