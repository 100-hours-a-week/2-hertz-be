package com.hertz.hertz_be.global.kafka.exception;

public class KafkaException extends RuntimeException {

    public KafkaException(String message) {
        super(message);
    }

    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }
}
