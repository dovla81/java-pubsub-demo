package com.example.pubsub;

import com.example.pubsub.model.*;
import com.example.pubsub.latency.RingBuffer;
import com.example.pubsub.monitoring.LatencyMonitor;
import com.example.pubsub.compliance.AuditLogger;
import com.example.pubsub.reliability.CircuitBreaker;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.time.Instant;

public class HighPerformanceTradeProcessor implements AutoCloseable {
    private final RingBuffer validationBuffer;
    private final RingBuffer pricingBuffer;
    private final RingBuffer riskBuffer;
    private final RingBuffer executionBuffer;
    
    private final ExecutorService validationPool;
    private final ExecutorService pricingPool;
    private final ExecutorService riskPool;
    private final ExecutorService executionPool;
    
    private final AuditLogger auditLogger;
    private final CircuitBreaker circuitBreaker;
    private final AtomicLong sequence;
    private volatile boolean running;
    
    public HighPerformanceTradeProcessor(int bufferSize, int poolSize) {
        // Initialize ring buffers
        this.validationBuffer = new RingBuffer(bufferSize);
        this.pricingBuffer = new RingBuffer(bufferSize);
        this.riskBuffer = new RingBuffer(bufferSize);
        this.executionBuffer = new RingBuffer(bufferSize);
        
        // Initialize thread pools with affinity
        this.validationPool = Executors.newFixedThreadPool(poolSize);
        this.pricingPool = Executors.newFixedThreadPool(poolSize);
        this.riskPool = Executors.newFixedThreadPool(poolSize);
        this.executionPool = Executors.newFixedThreadPool(poolSize);
        
        // Initialize support components
        this.auditLogger = new AuditLogger();
        this.circuitBreaker = new CircuitBreaker(10, 5000); // 10 failures, 5s reset
        this.sequence = new AtomicLong(0);
        this.running = true;
        
        // Start audit logger
        Thread auditThread = new Thread(auditLogger);
        auditThread.setName("audit-logger");
        auditThread.start();
        
        // Start processing chains
        startProcessingChains(poolSize);
    }
    
    private void startProcessingChains(int poolSize) {
        // Start validation workers
        for (int i = 0; i < poolSize; i++) {
            validationPool.submit(() -> {
                while (running) {
                    long seq = sequence.get();
                    MarketTrade trade = validationBuffer.poll(seq);
                    if (trade != null) {
                        processValidation(trade);
                    }
                }
            });
        }
        
        // Similar for other stages...
        // (Pricing, Risk, Execution stages would be implemented similarly)
    }
    
    private void processValidation(MarketTrade trade) {
        long startTime = System.nanoTime();
        try {
            if (!circuitBreaker.allowRequest()) {
                trade.reject("Circuit breaker open");
                return;
            }
            
            // Validate trade
            if (isValidTrade(trade)) {
                trade.setStatus(TradeStatus.VALIDATED);
                pricingBuffer.offer(trade, sequence.incrementAndGet());
                circuitBreaker.recordSuccess();
            } else {
                trade.reject("Validation failed");
            }
            
            // Record metrics
            LatencyMonitor.recordLatency("validation", startTime);
            LatencyMonitor.incrementCounter("processed");
            
            // Audit logging
            auditLogger.logTradeEvent(trade, "VALIDATION", 
                trade.getStatus() == TradeStatus.VALIDATED ? "SUCCESS" : "FAILED");
            
        } catch (Exception e) {
            circuitBreaker.recordFailure();
            LatencyMonitor.incrementCounter("errors");
            trade.reject("Validation error: " + e.getMessage());
            auditLogger.logTradeEvent(trade, "ERROR", e.getMessage());
        }
    }
    
    private boolean isValidTrade(MarketTrade trade) {
        return trade != null 
            && trade.getSymbol() != null 
            && !trade.getSymbol().isEmpty()
            && trade.getPrice() > 0 
            && trade.getQuantity() > 0
            && trade.getTrader() != null
            && trade.getAccount() != null;
    }
    
    public void submitTrade(String symbol, double price, int quantity, 
                          String venue, OrderType orderType, String counterparty,
                          String trader, String account) {
        MarketTrade trade = MarketTrade.create(symbol, price, quantity, 
                                             venue, orderType, counterparty,
                                             trader, account);
                                             
        long seq = sequence.incrementAndGet();
        if (!validationBuffer.offer(trade, seq)) {
            trade.reject("System at capacity");
            auditLogger.logTradeEvent(trade, "REJECTED", "Buffer full");
            LatencyMonitor.incrementCounter("rejected");
        }
    }
    
    @Override
    public void close() {
        running = false;
        validationPool.shutdown();
        pricingPool.shutdown();
        riskPool.shutdown();
        executionPool.shutdown();
        auditLogger.stop();
    }
    
    public void printMetrics() {
        LatencyMonitor.logMetrics();
        System.out.println("\nCircuit Breaker State: " + circuitBreaker.getState());
    }
    
    public static void main(String[] args) {
        try (HighPerformanceTradeProcessor processor = 
                new HighPerformanceTradeProcessor(1024, 4)) {
            
            // Warm up the system
            for (int i = 0; i < 1000; i++) {
                processor.submitTrade("WARM", 100.0, 100, "TEST", 
                    OrderType.MARKET, "WARMUP", "TRADER1", "ACC1");
            }
            
            LatencyMonitor.reset(); // Reset metrics after warmup
            
            // Process some real trades
            for (int i = 0; i < 10000; i++) {
                processor.submitTrade("AAPL", 150.50 + i, 100, "NYSE", 
                    OrderType.MARKET, "CP1", "TRADER1", "ACC1");
                processor.submitTrade("GOOGL", 2750.00 + i, 50, "NASDAQ", 
                    OrderType.LIMIT, "CP2", "TRADER2", "ACC2");
            }
            
            Thread.sleep(1000); // Let processing complete
            processor.printMetrics();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
