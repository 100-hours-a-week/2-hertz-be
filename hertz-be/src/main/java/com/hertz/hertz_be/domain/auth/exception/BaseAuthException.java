package com.hertz.hertz_be.domain.auth.exception;

public abstract class BaseAuthException extends RuntimeException {
    public abstract String getCode();
    public BaseAuthException(String message) {
        super(message);
    }
}
