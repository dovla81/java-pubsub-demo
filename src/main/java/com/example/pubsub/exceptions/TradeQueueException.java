package com.example.pubsub.exceptions;

public class TradeQueueException extends TradeProcessingException {
    public TradeQueueException(String message) {
        super(message);
    }

    public TradeQueueException(String message, Throwable cause) {
        super(message, cause);
    }
}
