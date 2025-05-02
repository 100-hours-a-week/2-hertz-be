package com.hertz.hertz_be.global.common;

public class ResponseCode {

    // 공통 예외 응답 code
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";

    // RT 관련 응답 code
    public static final String ACCESS_TOKEN_REISSUED = "ACCESS_TOKEN_REISSUED";
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";

    // 개인정보 등록 응답 code
    public static final String PROFILE_SAVED_SUCCESSFULLY = "PROFILE_SAVED_SUCCESSFULLY";
    public static final String DUPLICATE_NICKNAME = "DUPLICATE_NICKNAME";
    public static final String NICKNAME_CREATED = "NICKNAME_CREATED";
    public static final String NICKNAME_API_FAILED = "NICKNAME_API_FAILED";

    // 취향 선택 응답 code
    public static final String INTERESTS_SAVED_SUCCESSFULLY = "INTERESTS_SAVED_SUCCESSFULLY";
    public static final String EMPTY_LIST_NOT_ALLOWED = "EMPTY_LIST_NOT_ALLOWED";

}
