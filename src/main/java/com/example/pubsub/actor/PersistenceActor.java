package com.example.pubsub.actor;

import com.example.pubsub.Trade;

public class PersistenceActor extends TradeActor {
    @Override
    protected void process(Trade trade) {
        // Simulate persisting trade to database
        System.out.println("Persisting trade to database: " + trade);
        // In a real implementation, you would save to a database here
        forward(trade);
    }
}
