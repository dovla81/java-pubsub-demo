package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;

public class PooledValidationActor extends PooledActor {
    
    public PooledValidationActor(int actorId, BlockingQueue<Trade> inQueue) {
        super(actorId, inQueue);
    }

    @Override
    protected void process(Trade trade) {
        if (isValidTrade(trade)) {
            System.out.printf("Validator-%d: Trade validated: %s%n", actorId, trade);
            forward(trade);
        } else {
            System.out.printf("Validator-%d: Invalid trade rejected: %s%n", actorId, trade);
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
