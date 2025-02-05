package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;

@FunctionalInterface
public interface ActorFactory {
    PooledActor createActor(int actorId, BlockingQueue<Trade> sharedQueue);
}
