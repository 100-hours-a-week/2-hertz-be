package com.hertz.hertz_be.domain.auth.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthResponseCode {

    ACCESS_TOKEN_REISSUED(HttpStatus.OK, "ACCESS_TOKEN_REISSUED", "Access Token이 재발급되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "REFRESH_TOKEN_INVALID", "Refresh Token이 유효하지 않거나 만료되었습니다. 다시 로그인 해주세요."),
    LOGOUT_SUCCESS(HttpStatus.OK, "LOGOUT_SUCCESS", "정상적으로 로그아웃되었습니다."),
    USER_DELETE_SUCCESS(HttpStatus.OK, "USER_DELETE_SUCCESS", "사용자가 정상적으로 삭제되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
