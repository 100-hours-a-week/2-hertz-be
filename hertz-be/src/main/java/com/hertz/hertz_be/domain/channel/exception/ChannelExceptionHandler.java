package com.hertz.hertz_be.domain.channel.exception;

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
            CannotSendSignalToSelfException.class,
            AlreadyExitedChannelRoomException.class
    })
    public ResponseEntity<ResponseDto<Void>> badRequestException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            UserNotFoundException.class
    })
    public ResponseEntity<ResponseDto<Void>> notFoundException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            ForbiddenChannelException.class
    })
    public ResponseEntity<ResponseDto<Void>> forbiddenException(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    // Todo. 300번대로 리팩토링 필요
    @ExceptionHandler({
            InterestsNotSelectedException.class
    })
    public ResponseEntity<ResponseDto<Void>> ok(RuntimeException ex) {
        String code = ((BaseChannelException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }
}
