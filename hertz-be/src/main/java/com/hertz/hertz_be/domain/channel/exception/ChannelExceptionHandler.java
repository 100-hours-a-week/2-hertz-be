package com.hertz.hertz_be.domain.channel.exception;

import com.hertz.hertz_be.domain.auth.exception.BaseAuthException;
import com.hertz.hertz_be.domain.auth.exception.RateLimitException;
import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hertz.hertz_be.domain.channel")
public class ChannelExceptionHandler {
    @ExceptionHandler({
            AlreadyInConversationException.class
    })
    public ResponseEntity<ResponseDto<Void>> conflictException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            UserWithdrawnException.class
    })
    public ResponseEntity<ResponseDto<Void>> goneException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.GONE)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            InterestsNotSelectedException.class,
            CannotSendSignalToSelfException.class
    })
    public ResponseEntity<ResponseDto<Void>> badRequestException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }
}
