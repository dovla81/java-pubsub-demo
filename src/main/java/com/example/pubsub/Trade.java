package com.example.pubsub;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

public final class Trade {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);
    private final long id;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final LocalDateTime timestamp;

    public Trade(String symbol, double price, int quantity, LocalDateTime timestamp) {
        this.id = ID_GENERATOR.incrementAndGet();
        if (symbol == null || timestamp == null) {
            throw new IllegalArgumentException("Symbol and timestamp cannot be null");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    public Trade(String symbol, double price, int quantity) {
        this(symbol, price, quantity, LocalDateTime.now());
    }

    public String getSymbol() {
        return symbol;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp; // LocalDateTime is immutable, safe to return directly
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("Trade{id=%d, symbol='%s', price=%.2f, quantity=%d, timestamp=%s}",
            id, symbol, price, quantity, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trade trade = (Trade) o;
        return id == trade.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
