package com.hertz.hertz_be.domain.channel.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChannelResponseCode {

    // 성공 응답 코드
    SIGNAL_ROOM_CREATED(HttpStatus.CREATED, "SIGNAL_ROOM_CREATED", "시그널 채널 방이 성공적으로 생성되었습니다."),
    CHANNEL_ROOM_EXIT_SUCCESS(HttpStatus.OK, "CHANNEL_ROOM_EXIT_SUCCESS", "채널 방에서 성공적으로 퇴장했습니다."),
    CHANNEL_ROOM_LIST_FETCHED(HttpStatus.OK, "CHANNEL_ROOM_LIST_FETCHED", "채널 방 리스트가 성공적으로 조회되었습니다."),
    MESSAGE_CREATED(HttpStatus.CREATED, "MESSAGE_CREATED", "메시지가 성공적으로 생성되었습니다."),
    MATCH_SUCCESS(HttpStatus.OK, "MATCH_SUCCESS", "매칭이 성공적으로 완료되었습니다."),
    MATCH_PENDING(HttpStatus.OK, "MATCH_PENDING", "매칭이 대기 상태입니다."),
    MATCH_REJECTION_SUCCESS(HttpStatus.OK, "MATCH_REJECTION_SUCCESS", "매칭 거절이 성공적으로 처리되었습니다."),
    TUNING_SUCCESS(HttpStatus.OK, "TUNING_SUCCESS", "튜닝이 성공적으로 완료되었습니다."),
    TUNING_SUCCESS_BUT_NO_MATCH(HttpStatus.OK, "TUNING_SUCCESS_BUT_NO_MATCH", "튜닝은 성공했으나 매칭 상대가 없습니다."),

    // 예외 응답 코드
    USER_DEACTIVATED(HttpStatus.GONE, "USER_DEACTIVATED", "상대방이 탈퇴한 사용자입니다."),
    ALREADY_IN_CONVERSATION(HttpStatus.CONFLICT, "ALREADY_IN_CONVERSATION", "이미 대화 중입니다."),
    ALREADY_EXITED_CHANNEL_ROOM(HttpStatus.BAD_REQUEST, "ALREADY_EXITED_CHANNEL_ROOM", "이미 퇴장한 채널 방입니다."),
    USER_INTERESTS_NOT_SELECTED(HttpStatus.BAD_REQUEST, "USER_INTERESTS_NOT_SELECTED", "사용자가 관심사를 선택하지 않았습니다."),
    NO_TUNING_CANDIDATE(HttpStatus.NOT_FOUND, "NO_TUNING_CANDIDATE", "추천할 튜닝 후보자가 없습니다."),
    NO_CHANNEL_ROOM(HttpStatus.NOT_FOUND, "NO_CHANNEL_ROOM", "채널 방이 존재하지 않습니다."),
    MATCH_FAILED(HttpStatus.BAD_REQUEST, "MATCH_FAILED", "매칭에 실패했습니다."),
    TUNING_BAD_REQUEST(HttpStatus.BAD_REQUEST, "TUNING_BAD_REQUEST", "튜닝 요청이 잘못되었습니다."),
    TUNING_NOT_FOUND_USER(HttpStatus.NOT_FOUND, "TUNING_NOT_FOUND_USER", "튜닝 대상 사용자를 찾을 수 없습니다."),
    TUNING_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TUNING_INTERNAL_SERVER_ERROR", "튜닝 중 서버 오류가 발생했습니다."),
    TUNING_NOT_FOUND_DATA(HttpStatus.NOT_FOUND, "TUNING_NOT_FOUND_DATA", "튜닝 데이터가 존재하지 않습니다."),
    TUNING_NOT_FOUND_LIST(HttpStatus.NOT_FOUND, "TUNING_NOT_FOUND_LIST", "튜닝 리스트가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
