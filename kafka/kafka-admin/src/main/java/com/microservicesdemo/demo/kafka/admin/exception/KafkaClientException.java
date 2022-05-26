package com.microservicesdemo.demo.kafka.admin.exception;

import org.apache.kafka.common.protocol.types.Field;

public class KafkaClientException extends RuntimeException{
    public KafkaClientException() {
    }

    public KafkaClientException(String message) {
        super(message);
    }

    public KafkaClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
