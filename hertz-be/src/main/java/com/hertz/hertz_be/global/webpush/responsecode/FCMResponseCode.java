package com.hertz.hertz_be.global.webpush.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FCMResponseCode {

    FCM_TOKEN_SAVED_SUCCESS(HttpStatus.OK, "FCM_TOKEN_SAVED_SUCCESS", "FCM token 이 성공적으로 저장되었습니다."),
    FCM_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "FCM_INVALID_TOKEN", "FCM token 이 유효하지 않습니다."),
    FCM_TOKEN_SAVED_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "FCM_TOKEN_SAVED_FAIL", "FCM token 저장이 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
