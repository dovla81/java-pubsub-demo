package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.LinkedBlockingQueue;
import java.time.LocalDateTime;

public class PooledTradeProcessor implements AutoCloseable {
    private final ActorPool validationPool;
    private final ActorPool pricingPool;
    private final ActorPool persistencePool;
    private final LinkedBlockingQueue<Trade> validationQueue;
    private final LinkedBlockingQueue<Trade> pricingQueue;
    private final LinkedBlockingQueue<Trade> persistenceQueue;

    public PooledTradeProcessor(int validatorCount, int pricingCount, int persistenceCount) {
        // Create shared queues for each stage
        this.validationQueue = new LinkedBlockingQueue<>();
        this.pricingQueue = new LinkedBlockingQueue<>();
        this.persistenceQueue = new LinkedBlockingQueue<>();

        // Create actor pools
        this.validationPool = new ActorPool("Validation", validatorCount, validationQueue,
            (id, queue) -> new PooledValidationActor(id, queue));
        
        this.pricingPool = new ActorPool("Pricing", pricingCount, pricingQueue,
            (id, queue) -> new PooledPricingActor(id, queue));
        
        this.persistencePool = new ActorPool("Persistence", persistenceCount, persistenceQueue,
            (id, queue) -> new PooledPersistenceActor(id, queue));

        // Connect the pools
        validationPool.setNextPool(pricingPool);
        pricingPool.setNextPool(persistencePool);

        // Start all pools
        validationPool.start();
        pricingPool.start();
        persistencePool.start();
    }

    public void processTrade(String symbol, double price, int quantity) {
        Trade trade = new Trade(symbol, price, quantity, LocalDateTime.now());
        validationQueue.offer(trade);
    }

    @Override
    public void close() {
        validationPool.shutdown();
        pricingPool.shutdown();
        persistencePool.shutdown();
    }

    public static void main(String[] args) {
        try (PooledTradeProcessor processor = new PooledTradeProcessor(3, 2, 2)) {
            // Process some sample trades
            for (int i = 0; i < 10; i++) {
                processor.processTrade("AAPL", 150.50 + i, 100 + i * 10);
                processor.processTrade("GOOGL", 2750.00 + i, 50 + i * 5);
                processor.processTrade("MSFT", 300.75 + i, 200 + i * 15);
            }
            
            // Let the trades process
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
