package com.hertz.hertz_be.domain.channel.exception;

public abstract class BaseChannelException extends RuntimeException{
    public abstract String getCode();
    public BaseChannelException(String message) {
        super(message);
    }
}