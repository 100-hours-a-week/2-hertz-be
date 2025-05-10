package com.hertz.hertz_be.domain.interests.exception;

public abstract class BaseInterestsException extends RuntimeException{
    public abstract String getCode();
    public BaseInterestsException(String message) {
        super(message);
    }
}
