package com.hertz.hertz_be.domain.channel.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChannelResponseCode {

    // 성공 응답 코드
    SIGNAL_ROOM_CREATED(HttpStatus.CREATED, "SIGNAL_ROOM_CREATED", "시그널 룸이 성공적으로 생성되었습니다."),
    CHANNEL_ROOM_EXIT_SUCCESS(HttpStatus.OK, "CHANNEL_ROOM_EXIT_SUCCESS", "채널 방에서 성공적으로 퇴장했습니다."),
    CHANNEL_ROOM_LIST_FETCHED(HttpStatus.OK, "CHANNEL_ROOM_LIST_FETCHED", "채널 방 리스트가 성공적으로 조회되었습니다."),
    CHANNEL_ROOM_SUCCESS(HttpStatus.OK,"CHANNEL_ROOM_SUCCESS","채널방이 정상적으로 조회되었습니다."),
    NO_CHANNEL_ROOM(HttpStatus.OK, "NO_CHANNEL_ROOM", "채널 방이 존재하지 않습니다."),
    MESSAGE_CREATED(HttpStatus.CREATED, "MESSAGE_CREATED", "메시지가 성공적으로 생성되었습니다."),
    MATCH_SUCCESS(HttpStatus.OK, "MATCH_SUCCESS", "매칭이 성공적으로 완료되었습니다."),
    MATCH_PENDING(HttpStatus.ACCEPTED, "MATCH_PENDING", "상대방의 응답을 기다리는 중입니다."),
    MATCH_REJECTION_SUCCESS(HttpStatus.OK, "MATCH_REJECTION_SUCCESS", "매칭 거절이 성공적으로 처리되었습니다."),
    TUNING_SUCCESS(HttpStatus.OK, "TUNING_SUCCESS", "튜닝이 성공적으로 완료되었습니다."),
    NO_TUNING_CANDIDATE(HttpStatus.OK, "NO_TUNING_CANDIDATE", "추천할 튜닝 후보자가 없습니다."),
    TUNING_SUCCESS_BUT_NO_MATCH(HttpStatus.OK, "TUNING_SUCCESS_BUT_NO_MATCH", "튜닝은 성공했으나 매칭 상대가 없습니다."),

    // 예외 응답 코드
    CHANNEL_NOT_FOUND(HttpStatus.GONE, "CHANNEL_NOT_FOUND", "찾을 수 없는 채팅방입니다."),
    ALREADY_IN_CONVERSATION(HttpStatus.CONFLICT, "ALREADY_IN_CONVERSATION", "이미 대화 중입니다."),
    ALREADY_EXITED_CHANNEL_ROOM(HttpStatus.BAD_REQUEST, "ALREADY_EXITED_CHANNEL_ROOM", "이미 퇴장한 채널 방입니다."),
    USER_INTERESTS_NOT_SELECTED(HttpStatus.BAD_REQUEST, "USER_INTERESTS_NOT_SELECTED", "사용자가 관심사를 선택하지 않았습니다."),
    MATCH_FAILED(HttpStatus.CONFLICT, "MATCH_FAILED", "매칭에 실패했습니다."),
    TUNING_BAD_REQUEST(HttpStatus.BAD_REQUEST, "TUNING_BAD_REQUEST", "AI 서버에서 bad request 발생했습니다."),
    TUNING_NOT_FOUND_USER(HttpStatus.BAD_REQUEST, "TUNING_NOT_FOUND_USER", "AI 서버에서 사용자를 찾지 못했습니다."),
    TUNING_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TUNING_INTERNAL_SERVER_ERROR", "AI 서버 오류 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
