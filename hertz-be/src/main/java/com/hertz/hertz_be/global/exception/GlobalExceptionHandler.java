package com.hertz.hertz_be.global.exception;

import com.hertz.hertz_be.domain.user.exception.UserException;
import com.hertz.hertz_be.global.common.NewResponseCode;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.sql.SQLException;

@Slf4j
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

    // TODO(yunbin): tuningreport 도메인까지 리팩토링 완료되면 위에 있는 기존 공통 예외 로직 삭제할 것

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseDto<Void>> handleBusinessException(BusinessException e) {
        log.warn("[비즈니스 로직 에러 발생] {}", e.getMessage());
        return ResponseEntity
                .status(e.getStatus())
                .body(new ResponseDto<>(e.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleUnexpectedException(Exception e) {
        log.warn("[비즈니스에서 잡지 못하는 에러 발생] {}", e.getMessage());

        return ResponseEntity
                .status(NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(new ResponseDto<>(
                        NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                        NewResponseCode.INTERNAL_SERVER_ERROR.getMessage(),
                        null
                ));
    }

    @ExceptionHandler({ DataAccessException.class, SQLException.class })
    public ResponseEntity<ResponseDto<Void>> handleDatabaseException(Exception e) {
        log.warn("[DB 에러] {}", e.getMessage());

        return ResponseEntity
                .status(NewResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(new ResponseDto<>(
                        NewResponseCode.INTERNAL_SERVER_ERROR.getCode(),
                        NewResponseCode.INTERNAL_SERVER_ERROR.getMessage(),
                        null
                ));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ResponseDto<Void>> handleValidationException(Exception e) {
        log.warn("[요청 HTTP BODY 검증 에러] {}", e.getMessage());

        return ResponseEntity
                .status(NewResponseCode.BAD_REQUEST.getHttpStatus())
                .body(new ResponseDto<>(
                        NewResponseCode.BAD_REQUEST.getCode(),
                        NewResponseCode.BAD_REQUEST.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseDto<Void>> handleMethodNotSupported(Exception e) {
        log.warn("[요청 HTTP URL 검증 에러] {}", e.getMessage());

        return ResponseEntity
                .status(NewResponseCode.NOT_IMPLEMENTED.getHttpStatus())
                .body(new ResponseDto<>(
                        NewResponseCode.NOT_IMPLEMENTED.getCode(),
                        NewResponseCode.NOT_IMPLEMENTED.getMessage(),
                        null
                ));
    }

}
