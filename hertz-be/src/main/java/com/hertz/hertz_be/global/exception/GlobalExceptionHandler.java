package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.auth.exception.RefreshTokenInvalidException;
import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import com.hertz.hertz_be.global.common.dto.ApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleDuplicateNickname(UserException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponseDto.error(
                        "DUPLICATE_NICKNAME",
                        "이미 사용 중인 닉네임입니다. 다른 닉네임을 선택해주세요."
                ));
    }
}
