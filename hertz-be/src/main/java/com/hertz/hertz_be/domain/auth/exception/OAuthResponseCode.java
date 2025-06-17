package com.hertz.hertz_be.domain.auth.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OAuthResponseCode {

    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER", "지원하지 않는 OAuth Provider 입니다."),
    OAUTH_STATE_INVALID(HttpStatus.BAD_REQUEST, "OAUTH_STATE_INVALID", "유효하지 않은 OAuth 상태 값입니다."),
    RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT", "OAuth 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    USER_ALREADY_REGISTERED(HttpStatus.CONFLICT, "USER_ALREADY_REGISTERED", "로그인에 성공했습니다."),
    USER_NOT_REGISTERED(HttpStatus.NOT_FOUND, "USER_NOT_REGISTERED", "신규 회원입니다."),
    USER_INTERESTS_NOT_SELECTED(HttpStatus.OK, "USER_INTERESTS_NOT_SELECTED", "사용자가 아직 취향 선택을 완료하지 않았습니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
