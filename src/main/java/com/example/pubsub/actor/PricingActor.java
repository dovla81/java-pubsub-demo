package com.example.pubsub.actor;

import com.example.pubsub.Trade;

public class PricingActor extends TradeActor {
    @Override
    protected void process(Trade trade) {
        // Simulate price calculation
        double totalValue = trade.getPrice() * trade.getQuantity();
        System.out.println("Calculated total value for " + trade.getSymbol() + ": $" + totalValue);
        forward(trade);
    }
}
