package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class) // Todo: User 도메인에 있는 exception으로 위치 수정 필요
    public ResponseEntity handleUserException(UserException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto(ex.getCode(), ex.getMessage(), null));
    }

    // DTO @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(ResponseCode.BAD_REQUEST, "잘못된 요청입니다.", null));
    }

    // JSON 형식 오류 (e.g. null, 오타 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(ResponseCode.BAD_REQUEST, "잘못된 요청입니다.", null));
    }

    // 경로 변수나 쿼리 파라미터 @Valid 실패 시
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(ResponseCode.BAD_REQUEST, "잘못된 요청입니다.", null));
    }
}
