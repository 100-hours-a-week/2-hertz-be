package com.hertz.hertz_be.auth.exception;

import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hertz.hertz_be.auth") // TODO: 병합 이후에 경로 수정
public class RefreshTokenExceptionHandler {
    @ExceptionHandler(RefreshTokenInvalidException.class)
    public ResponseEntity<?> handleRefreshTokenError(RefreshTokenInvalidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(ex.getCode(), ex.getMessage(), null));
    }
}
