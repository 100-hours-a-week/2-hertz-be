package com.hertz.hertz_be.global.common;

public class ResponseCode {

    // 공통 예외 응답 code
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String ACCESS_TOKEN_EXPIRED = "ACCESS_TOKEN_EXPIRED";
    public static final String NOT_IMPLEMENTED = "NOT_IMPLEMENTED";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String AI_SERVER_ERROR = "AI_SERVER_ERROR";

    // OAuth 관련 응답 code
    public static final String UNSUPPORTED_PROVIDER = "UNSUPPORTED_PROVIDER";
    public static final String OAUTH_STATE_INVALID = "OAUTH_STATE_INVALID";
    public static final String RATE_LIMIT = "RATE_LIMIT";
    public static final String USER_ALREADY_REGISTERED = "USER_ALREADY_REGISTERED";
    public static final String USER_NOT_REGISTERED = "USER_NOT_REGISTERED";

    /**
     * Auth 관련 응답 code
     */
    public static final String ACCESS_TOKEN_REISSUED = "ACCESS_TOKEN_REISSUED";
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String LOGOUT_SUCCESS = "LOGOUT_SUCCESS";

    // 개인정보 등록 응답 code
    public static final String PROFILE_SAVED_SUCCESSFULLY = "PROFILE_SAVED_SUCCESSFULLY";
    public static final String DUPLICATE_USER = "DUPLICATE_USER";
    public static final String DUPLICATE_NICKNAME = "DUPLICATE_NICKNAME";
    public static final String NICKNAME_CREATED = "NICKNAME_CREATED";
    public static final String NICKNAME_API_FAILED = "NICKNAME_API_FAILED";
    public static final String NICKNAME_GENERATION_TIMEOUT = "NICKNAME_GENERATION_TIMEOUT";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";


    // 취향 선택 응답 code
    public static final String INTERESTS_SAVED_SUCCESSFULLY = "INTERESTS_SAVED_SUCCESSFULLY";
    public static final String EMPTY_LIST_NOT_ALLOWED = "EMPTY_LIST_NOT_ALLOWED";

    //채널의 시그널 관련 응답 code
    public static final String SIGNAL_ROOM_CREATED = "SIGNAL_ROOM_CREATED";
    public static final String USER_DEACTIVATED = "USER_DEACTIVATED";
    public static final String ALREADY_IN_CONVERSATION = "ALREADY_IN_CONVERSATION";
    public static final String NEW_MESSAGE = "NEW_MESSAGE";
    public static final String NO_ANY_NEW_MESSAGE = "NO_ANY_NEW_MESSAGE";
    public static final String CHANNEL_ROOM_EXIT_SUCCESS = "CHANNEL_ROOM_EXIT_SUCCESS";
    public static final String ALREADY_EXITED_CHANNEL_ROOM = "ALREADY_EXITED_CHANNEL_ROOM";

    // 튜닝 추천 상대 반환 로직 관련 응답 code
    public static final String USER_INTERESTS_NOT_SELECTED = "USER_INTERESTS_NOT_SELECTED";
    public static final String NO_TUNING_CANDIDATE = "NO_TUNING_CANDIDATE";


    // 채널 정보 관련 응답 code
    public static final String CHANNEL_ROOM_LIST_FETCHED = "CHANNEL_ROOM_LIST_FETCHED";
    public static final String NO_CHANNEL_ROOM = "NO_CHANNEL_ROOM";
    public static final String MESSAGE_CREATED = "MESSAGE_CREATED";

    // 매칭 관련 응답
    public static final String MATCH_SUCCESS = "MATCH_SUCCESS";
    public static final String MATCH_FAILED = "MATCH_FAILED";
    public static final String MATCH_PENDING = "MATCH_PENDING";
    public static final String MATCH_REJECTION_SUCCESS = "MATCH_REJECTION_SUCCESS";


    /* AI Response */
    public static final String EMBEDDING_REGISTER_SUCCESS = "EMBEDDING_REGISTER_SUCCESS";
    public static final String EMBEDDING_REGISTER_BAD_REQUEST = "EMBEDDING_REGISTER_BAD_REQUEST";
    public static final String EMBEDDING_CONFLICT_DUPLICATE_ID = "EMBEDDING_CONFLICT_DUPLICATE_ID";
    public static final String BAD_REQUEST_VALIDATION_ERROR = "BAD_REQUEST_VALIDATION_ERROR";
    public static final String EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED = "EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED";
    public static final String EMBEDDING_REGISTER_SERVER_ERROR = "EMBEDDING_REGISTER_SERVER_ERROR";

    /**
     * 알림창 관련 응답 code
     */
    public static final String NOTICE_CREATED_SUCCESS = "NOTICE_CREATED_SUCCESS";
    public static final String ALARM_FETCH_SUCCESS = "ALARM_FETCH_SUCCESS";
    public static final String NO_ALARMS = "NO_ALARMS";
    public static final String ALARM_DELETE_SUCCESS = "ALARM_DELETE_SUCCESS";
    /**
     * 매칭 수락/거절 관련 응답 code
     */
    public static final String TUNING_SUCCESS = "TUNING_SUCCESS";
    public static final String TUNING_SUCCESS_BUT_NO_MATCH = "TUNING_SUCCESS_BUT_NO_MATCH";
    public static final String TUNING_BAD_REQUEST = "TUNING_BAD_REQUEST";
    public static final String TUNING_NOT_FOUND_USER = "TUNING_NOT_FOUND_USER";
    public static final String TUNING_INTERNAL_SERVER_ERROR = "TUNING_INTERNAL_SERVER_ERROR";

    public static final String TUNING_NOT_FOUND_DATA = "TUNING_NOT_FOUND_DATA";
    public static final String TUNING_NOT_FOUND_LIST = "TUNING_NOT_FOUND_LIST";


    /* 마이페이지, 상대방 상세 페이지 조회 */
    public static final String USER_INFO_FETCH_SUCCESS = "USER_INFO_FETCH_SUCCESS";
    public static final String OTHER_USER_INFO_FETCH_SUCCESS = "OTHER_USER_INFO_FETCH_SUCCESS";

    /**
     * User 삭제 관련 응답 code
     */
    public static final String USER_DELETE_SUCCESS = "USER_DELETE_SUCCESS";

    // 튜닝 리포트 관련 응답 code
    public static final String REPORT_LIST_FETCH_SUCCESS = "REPORT_LIST_FETCH_SUCCESS";
    public static final String NO_REPORTS = "NO_REPORTS";
    public static final String REACTION_ADDED = "REACTION_ADDED";
    public static final String REACTION_REMOVED = "REACTION_REMOVED";
    public static final String DELETED_REPORT = "DELETED_REPORT";


}
