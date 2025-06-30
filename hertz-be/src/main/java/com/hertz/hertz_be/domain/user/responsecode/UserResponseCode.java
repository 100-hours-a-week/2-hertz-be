package com.hertz.hertz_be.domain.user.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


@Getter
@RequiredArgsConstructor
public enum UserResponseCode {

    // 예외 응답 코드
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자가 존재하지 않습니다."),
    USER_DEACTIVATED(HttpStatus.GONE, "USER_DEACTIVATED", "상대방이 탈퇴한 사용자입니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "DUPLICATE_USER", "이미 등록된 사용자입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다."),
    NICKNAME_API_FAILED(HttpStatus.BAD_GATEWAY, "NICKNAME_API_FAILED", "닉네임 생성 API 호출 실패"),
    NICKNAME_GENERATION_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "NICKNAME_GENERATION_TIMEOUT", "5초 내에 중복되지 않은 닉네임을 찾지 못했습니다."),
    WRONG_INVITATION_CODE(HttpStatus.BAD_REQUEST, "WRONG_INVITATION_CODE", "유효하지 않은 초대코드입니다."),
    CATEGORY_UPDATED_SUCCESSFULLY(HttpStatus.OK, "CATEGORY_UPDATED_SUCCESSFULLY", "사용자의 카테고리가 정상적으로 수정되었습니다."),
    CATEGORY_IS_REJECTED(HttpStatus.BAD_REQUEST, "CATEGORY_IS_REJECTED", "상대방이 시그널을 받고 싶지 않은 카테고리입니다."),
    PROFILE_UPDATED_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "PROFILE_UPDATED_UNAUTHORIZED", "자기 자신의 한 줄 소개만 수정할 수 있습니다."),

    // 성공 응답 코드
    USER_INFO_FETCH_SUCCESS(HttpStatus.OK, "USER_INFO_FETCH_SUCCESS", "사용자의 정보가 정상적으로 조회되었습니다."),
    OTHER_USER_INFO_FETCH_SUCCESS(HttpStatus.OK, "OTHER_USER_INFO_FETCH_SUCCESS", "상대방의 정보가 정상적으로 조회되었습니다."),
    PROFILE_UPDATED_SUCCESSFULLY(HttpStatus.OK, "PROFILE_UPDATED_SUCCESSFULLY", "사용자 프로필이 정상적으로 수정되었습니다."),
    PROFILE_SAVED_SUCCESSFULLY(HttpStatus.CREATED, "PROFILE_SAVED_SUCCESSFULLY", "개인정보가 정상적으로 저장되었습니다."),
    NICKNAME_CREATED(HttpStatus.CREATED, "NICKNAME_CREATED", "닉네임이 성공적으로 생성되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
