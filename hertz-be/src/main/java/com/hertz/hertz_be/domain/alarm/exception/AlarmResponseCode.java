package com.hertz.hertz_be.domain.alarm.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AlarmResponseCode {

    NOTICE_CREATED_SUCCESS(HttpStatus.CREATED, "NOTICE_CREATED_SUCCESS", "공지가 성공적으로 등록되었습니다."),
    ALARM_FETCH_SUCCESS(HttpStatus.OK, "ALARM_FETCH_SUCCESS", "알림이 정상적으로 조회되었습니다."),
    NO_ALARMS(HttpStatus.OK, "NO_ALARMS", "최근 30일 동안 새로운 알림이 없습니다."),
    ALARM_DELETE_SUCCESS(HttpStatus.OK, "ALARM_DELETE_SUCCESS", "알림이 성공적으로 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
