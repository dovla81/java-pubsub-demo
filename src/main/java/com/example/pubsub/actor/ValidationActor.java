package com.example.pubsub.actor;

import com.example.pubsub.Trade;

public class ValidationActor extends TradeActor {
    @Override
    protected void process(Trade trade) {
        // Validate trade
        if (isValidTrade(trade)) {
            System.out.println("Trade validated: " + trade);
            forward(trade);
        } else {
            System.out.println("Invalid trade rejected: " + trade);
        }
    }

    private boolean isValidTrade(Trade trade) {
        return trade != null 
            && trade.getSymbol() != null 
            && !trade.getSymbol().isEmpty()
            && trade.getPrice() > 0 
            && trade.getQuantity() > 0;
    }
}
