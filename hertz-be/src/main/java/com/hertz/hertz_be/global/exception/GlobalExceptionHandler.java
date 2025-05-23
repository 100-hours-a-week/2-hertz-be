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


    // 경로 변수나 쿼리 파라미터 @Valid 검증, DTO @Valid 검증, JSON 형식 오류 (e.g. null, 오타 등)
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ResponseDto<Void>> handleMessageNotReadable(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(ResponseCode.BAD_REQUEST, "잘못된 요청입니다.", null));
    }

    // 존재하지 않는 URL
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseDto<Void>> handleNotFound(NoHandlerFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ResponseDto<>(ResponseCode.NOT_FOUND, "존재하지 않는 API입니다.", null));
    }

//    // 비즈니스 로직에서 잡지못한 모든 예외 검증
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ResponseDto<Void>> handleException(Exception ex) {
//        return ResponseEntity
//                .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ResponseDto<>(ResponseCode.INTERNAL_SERVER_ERROR, "내부 서버에서 오류가 발생했습니다.", null));
//    }

    // 비즈니스 로직에서의 서버 문제 검증
    @ExceptionHandler(InternalServerErrorException.class)
    public ResponseEntity<ResponseDto<Void>> handleInternalServerError(InternalServerErrorException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto<>(
                        ex.getCode(),
                        ex.getMessage(),
                        null
                ));
    }

    // Ai 서버 문제 검증
    @ExceptionHandler(AiServerErrorException.class)
    public ResponseEntity<ResponseDto<Void>> handleAiServerError(AiServerErrorException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto<>(
                        ex.getCode(),
                        ex.getMessage(),
                        null
                ));
    }

    // HTTP 메서드(예: PATCH, PUT, DELETE)와 URI에 대해 컨트롤러가 존재 검증
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseDto<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(new ResponseDto<>(
                        ResponseCode.NOT_IMPLEMENTED,
                        "요청한 URI의 메소드에 대해 서버가 구현하고 있지 않습니다.",
                        null
                ));
    }

}
