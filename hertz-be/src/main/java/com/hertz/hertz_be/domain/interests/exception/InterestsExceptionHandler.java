package com.hertz.hertz_be.domain.interests.exception;

import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hertz.hertz_be.domain.interests")
public class InterestsExceptionHandler {

    @ExceptionHandler({
            RegisterBadRequestException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleInterestsBadRequestExceptions(RuntimeException ex) {
        String code = ((BaseInterestsException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            DuplicateIdException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleInterestsDuplicateExceptions(RuntimeException ex) {
        String code = ((BaseInterestsException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            SimilarityUpdateFailedException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleInterestsUpdateFailExceptions(RuntimeException ex) {
        String code = ((BaseInterestsException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            InvalidInterestsInputException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleInterestsUnprocessableContentExceptions(RuntimeException ex) {
        String code = ((BaseInterestsException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }
}
