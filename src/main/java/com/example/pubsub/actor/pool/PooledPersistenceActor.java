package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;

public class PooledPersistenceActor extends PooledActor {
    
    public PooledPersistenceActor(int actorId, BlockingQueue<Trade> inQueue) {
        super(actorId, inQueue);
    }

    @Override
    protected void process(Trade trade) {
        System.out.printf("Persistence-%d: Persisting trade to database: %s%n", 
            actorId, trade);
        // In a real implementation, you would save to a database here
        forward(trade);
    }
}
