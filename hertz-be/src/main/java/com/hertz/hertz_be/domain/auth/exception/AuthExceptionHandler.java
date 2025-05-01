package com.hertz.hertz_be.domain.auth.exception;

import com.hertz.hertz_be.global.common.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.hertz.hertz_be.domain.auth")
public class AuthExceptionHandler {

    @ExceptionHandler({
            RefreshTokenInvalidException.class,
            ProviderInvalidException.class,
            OAuthStateInvalidException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleAuthBadRequestExceptions(RuntimeException ex) {
        String code = ((BaseAuthException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

    @ExceptionHandler({
            RateLimitException.class
    })
    public ResponseEntity<ResponseDto<Void>> handleAuthTooManyRequestExceptions(RuntimeException ex) {
        String code = ((BaseAuthException) ex).getCode();
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ResponseDto<>(code, ex.getMessage(), null));
    }

}
