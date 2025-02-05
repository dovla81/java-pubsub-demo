package com.example.pubsub.actor;

import com.example.pubsub.Trade;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.time.LocalDateTime;

public class ActorTradeProcessor implements AutoCloseable {
    private final ValidationActor validationActor;
    private final PricingActor pricingActor;
    private final PersistenceActor persistenceActor;
    private final ExecutorService executorService;

    public ActorTradeProcessor() {
        this.validationActor = new ValidationActor();
        this.pricingActor = new PricingActor();
        this.persistenceActor = new PersistenceActor();
        
        // Set up the processing pipeline
        validationActor.setNextActor(pricingActor);
        pricingActor.setNextActor(persistenceActor);
        
        // Create thread pool for actors
        this.executorService = Executors.newFixedThreadPool(3);
        
        // Start all actors
        executorService.submit(validationActor);
        executorService.submit(pricingActor);
        executorService.submit(persistenceActor);
    }

    public void processTrade(String symbol, double price, int quantity) {
        Trade trade = new Trade(symbol, price, quantity, LocalDateTime.now());
        validationActor.send(trade);
    }

    @Override
    public void close() {
        validationActor.stop();
        pricingActor.stop();
        persistenceActor.stop();
        executorService.shutdown();
    }

    public static void main(String[] args) {
        try (ActorTradeProcessor processor = new ActorTradeProcessor()) {
            // Process some sample trades
            processor.processTrade("AAPL", 150.50, 100);
            processor.processTrade("GOOGL", 2750.00, 50);
            processor.processTrade("MSFT", 300.75, 200);
            
            // Let the trades process
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
