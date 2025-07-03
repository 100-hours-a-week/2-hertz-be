package com.hertz.hertz_be.global.kafka.exception;

public class KafkaSseDeliveryException extends RuntimeException {

    public KafkaSseDeliveryException(String message) {
        super(message);
    }

    public KafkaSseDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
