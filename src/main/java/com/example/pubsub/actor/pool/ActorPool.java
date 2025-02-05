package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActorPool {
    private final List<PooledActor> actors;
    private final ExecutorService executorService;
    private final String poolName;
    private final BlockingQueue<Trade> sharedQueue;
    private ActorPool nextPool;

    public ActorPool(String poolName, int poolSize, BlockingQueue<Trade> sharedQueue, 
                    ActorFactory actorFactory) {
        this.poolName = poolName;
        this.sharedQueue = sharedQueue;
        this.actors = new ArrayList<>(poolSize);
        this.executorService = Executors.newFixedThreadPool(poolSize);

        // Create the actors in the pool
        for (int i = 0; i < poolSize; i++) {
            PooledActor actor = actorFactory.createActor(i, sharedQueue);
            actors.add(actor);
        }
    }

    public void start() {
        for (PooledActor actor : actors) {
            executorService.submit(actor);
        }
    }

    public void setNextPool(ActorPool nextPool) {
        this.nextPool = nextPool;
        // Set the next pool for all actors
        for (PooledActor actor : actors) {
            actor.setNextQueue(nextPool != null ? nextPool.getSharedQueue() : null);
        }
    }

    public BlockingQueue<Trade> getSharedQueue() {
        return sharedQueue;
    }

    public void shutdown() {
        actors.forEach(PooledActor::stop);
        executorService.shutdown();
    }

    public String getPoolName() {
        return poolName;
    }
}
