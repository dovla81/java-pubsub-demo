package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;

public class PooledPricingActor extends PooledActor {
    
    public PooledPricingActor(int actorId, BlockingQueue<Trade> inQueue) {
        super(actorId, inQueue);
    }

    @Override
    protected void process(Trade trade) {
        double totalValue = trade.getPrice() * trade.getQuantity();
        System.out.printf("Pricing-%d: Calculated total value for %s: $%.2f%n", 
            actorId, trade.getSymbol(), totalValue);
        forward(trade);
    }
}
