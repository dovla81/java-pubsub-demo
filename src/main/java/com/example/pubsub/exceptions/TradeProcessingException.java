package com.example.pubsub.exceptions;

public class TradeProcessingException extends RuntimeException {
    public TradeProcessingException(String message) {
        super(message);
    }

    public TradeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
