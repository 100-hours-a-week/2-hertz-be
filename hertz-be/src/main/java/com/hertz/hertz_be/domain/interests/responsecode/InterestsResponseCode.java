package com.hertz.hertz_be.domain.interests.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InterestsResponseCode {

    // 성공 응답 코드
    INTERESTS_SAVED_SUCCESSFULLY(HttpStatus.CREATED, "INTERESTS_SAVED_SUCCESSFULLY", "사용자의 취향이 정상적으로 저장되었습니다."),

    // 에외 응답 코드
    EMPTY_LIST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "EMPTY_LIST_NOT_ALLOWED", "하나 이상의 항목을 선택해야 합니다."),
    USER_INTERESTS_NOT_SELECTED(HttpStatus.NOT_FOUND, "USER_INTERESTS_NOT_SELECTED", "사용자가 아직 취향 선택을 완료하지 않았습니다."),

    // AI 서버 에외 응답 코드
    EMBEDDING_REGISTER_SUCCESS(HttpStatus.CREATED, "EMBEDDING_REGISTER_SUCCESS", "임베딩이 성공적으로 등록되었습니다."),
    EMBEDDING_REGISTER_BAD_REQUEST(HttpStatus.BAD_REQUEST, "EMBEDDING_REGISTER_BAD_REQUEST", "임베딩 요청이 잘못되었습니다."),
    EMBEDDING_CONFLICT_DUPLICATE_ID(HttpStatus.CONFLICT, "EMBEDDING_CONFLICT_DUPLICATE_ID", "이미 등록된 사용자입니다."),
    BAD_REQUEST_VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "BAD_REQUEST_VALIDATION_ERROR", "유효하지 않은 요청입니다."),
    EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMBEDDING_REGISTER_SIMILARITY_UPDATE_FAILED", "임베딩 유사도 업데이트에 실패했습니다."),
    EMBEDDING_REGISTER_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EMBEDDING_REGISTER_SERVER_ERROR", "임베딩 서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
