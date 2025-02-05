package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class PooledActor implements Runnable {
    protected final BlockingQueue<Trade> inQueue;
    protected BlockingQueue<Trade> outQueue;
    protected final int actorId;
    protected volatile boolean running;
    private static final ConcurrentMap<Long, Boolean> processedTrades = new ConcurrentHashMap<>();

    public PooledActor(int actorId, BlockingQueue<Trade> inQueue) {
        this.actorId = actorId;
        this.inQueue = inQueue;
        this.running = true;
    }

    public void setNextQueue(BlockingQueue<Trade> outQueue) {
        this.outQueue = outQueue;
    }

    protected void forward(Trade trade) {
        if (outQueue != null) {
            outQueue.offer(trade);
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Trade trade = inQueue.take();
                // Check if this trade has been processed before
                if (processedTrades.putIfAbsent(trade.getId(), true) != null) {
                    System.out.printf("WARNING: Trade %d was already processed! This should never happen!%n", 
                        trade.getId());
                }
                process(trade);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    protected abstract void process(Trade trade);
}
