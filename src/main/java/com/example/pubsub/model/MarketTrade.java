package com.example.pubsub.model;

import java.time.Instant;
import java.util.UUID;

public class MarketTrade {
    private final UUID tradeId;
    private final String symbol;
    private final double price;
    private final int quantity;
    private final String venue;
    private final OrderType orderType;
    private final String counterparty;
    private final Instant receivedTime;
    private final long sequenceNumber;
    private final String trader;
    private final String account;
    private volatile TradeStatus status;
    private volatile String rejectionReason;
    
    // Object pooling for reduced GC
    private static final ThreadLocal<MarketTrade> TRADE_POOL = ThreadLocal.withInitial(() -> {
        MarketTrade[] trades = new MarketTrade[1024];
        for (int i = 0; i < trades.length; i++) {
            trades[i] = new MarketTrade();
        }
        return trades[0];
    });
    
    // Private constructor for object pooling
    private MarketTrade() {
        this.tradeId = UUID.randomUUID();
        this.symbol = "";
        this.price = 0;
        this.quantity = 0;
        this.venue = "";
        this.orderType = OrderType.MARKET;
        this.counterparty = "";
        this.receivedTime = Instant.now();
        this.sequenceNumber = 0;
        this.trader = "";
        this.account = "";
        this.status = TradeStatus.RECEIVED;
    }
    
    // Factory method to get a trade from the pool
    public static MarketTrade create(String symbol, double price, int quantity, 
                                   String venue, OrderType orderType, String counterparty,
                                   String trader, String account) {
        MarketTrade trade = TRADE_POOL.get();
        trade.reset(symbol, price, quantity, venue, orderType, counterparty, trader, account);
        return trade;
    }
    
    private void reset(String symbol, double price, int quantity, 
                      String venue, OrderType orderType, String counterparty,
                      String trader, String account) {
        // Reset mutable state
        this.status = TradeStatus.RECEIVED;
        this.rejectionReason = null;
        
        // Set new values
        this.symbol = symbol;
        this.price = price;
        this.quantity = quantity;
        this.venue = venue;
        this.orderType = orderType;
        this.counterparty = counterparty;
        this.trader = trader;
        this.account = account;
        this.receivedTime = Instant.now();
        this.sequenceNumber = TradeSequence.getNext();
    }
    
    // Getters
    public UUID getTradeId() { return tradeId; }
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getVenue() { return venue; }
    public OrderType getOrderType() { return orderType; }
    public String getCounterparty() { return counterparty; }
    public Instant getReceivedTime() { return receivedTime; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getTrader() { return trader; }
    public String getAccount() { return account; }
    public TradeStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    
    // Status management
    public void setStatus(TradeStatus status) {
        this.status = status;
    }
    
    public void reject(String reason) {
        this.status = TradeStatus.REJECTED;
        this.rejectionReason = reason;
    }
    
    @Override
    public String toString() {
        return String.format("MarketTrade{id=%s, symbol='%s', price=%.2f, quantity=%d, status=%s}",
            tradeId, symbol, price, quantity, status);
    }
}

enum OrderType {
    MARKET, LIMIT, STOP, STOP_LIMIT
}

enum TradeStatus {
    RECEIVED, VALIDATED, PRICED, RISK_CHECKED, EXECUTED, REJECTED, CANCELLED
}

class TradeSequence {
    private static long sequence = 0;
    
    public static synchronized long getNext() {
        return ++sequence;
    }
}
