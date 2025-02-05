package com.example.pubsub.exceptions;

public class TradeGenerationException extends TradeProcessingException {
    public TradeGenerationException(String message) {
        super(message);
    }

    public TradeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
