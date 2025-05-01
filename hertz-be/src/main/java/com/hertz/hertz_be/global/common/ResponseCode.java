package com.hertz.hertz_be.global.common;

public class ResponseCode {

    // 공통 예외 응답 code
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";

    // OAuth 관련 응답 code
    public static final String UNSUPPORTED_PROVIDER = "UNSUPPORTED_PROVIDER";
    public static final String OAUTH_STATE_INVALID = "OAUTH_STATE_INVALID";
    public static final String RATE_LIMIT = "RATE_LIMIT";
    public static final String USER_ALREADY_REGISTERED = "USER_ALREADY_REGISTERED";
    public static final String USER_NOT_REGISTERED = "USER_NOT_REGISTERED";

    // RT 관련 응답 code
    public static final String ACCESS_TOKEN_REISSUED = "ACCESS_TOKEN_REISSUED";
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";

}
