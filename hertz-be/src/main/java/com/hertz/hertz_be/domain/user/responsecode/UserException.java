package com.hertz.hertz_be.domain.user.responsecode;

import lombok.Getter;

@Getter
public class UserException extends RuntimeException {
    private final String code;

    public UserException (String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public UserException (String message, String code) {
        super(message);
        this.code = code;
    }
}
