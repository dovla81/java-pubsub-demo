package com.example.pubsub.compliance;

import com.example.pubsub.model.MarketTrade;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditLogger implements Runnable {
    private static final String AUDIT_DIR = "audit_logs";
    private static final int QUEUE_SIZE = 100_000;
    private final BlockingQueue<AuditEvent> auditQueue;
    private final AtomicBoolean running;
    private final String logFile;
    
    public AuditLogger() {
        this.auditQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
        this.running = new AtomicBoolean(true);
        this.logFile = AUDIT_DIR + "/audit_" + Instant.now().toString().replace(":", "_") + ".log";
        
        // Ensure audit directory exists
        try {
            Files.createDirectories(Paths.get(AUDIT_DIR));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create audit directory", e);
        }
    }
    
    public void logTradeEvent(MarketTrade trade, String event, String details) {
        AuditEvent auditEvent = new AuditEvent(
            trade.getTradeId().toString(),
            trade.getSymbol(),
            trade.getPrice(),
            trade.getQuantity(),
            trade.getTrader(),
            trade.getAccount(),
            event,
            details,
            Instant.now()
        );
        
        if (!auditQueue.offer(auditEvent)) {
            System.err.println("WARNING: Audit queue full, event dropped");
        }
    }
    
    @Override
    public void run() {
        while (running.get()) {
            try {
                AuditEvent event = auditQueue.take();
                String logLine = formatAuditEvent(event);
                Files.write(Paths.get(logFile), 
                          (logLine + System.lineSeparator()).getBytes(),
                          StandardOpenOption.CREATE, 
                          StandardOpenOption.APPEND);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error writing audit log: " + e.getMessage());
            }
        }
    }
    
    private String formatAuditEvent(AuditEvent event) {
        return String.format("%s|%s|%s|%.4f|%d|%s|%s|%s|%s",
            event.timestamp,
            event.tradeId,
            event.symbol,
            event.price,
            event.quantity,
            event.trader,
            event.account,
            event.event,
            event.details
        );
    }
    
    public void stop() {
        running.set(false);
    }
    
    private static class AuditEvent {
        final String tradeId;
        final String symbol;
        final double price;
        final int quantity;
        final String trader;
        final String account;
        final String event;
        final String details;
        final Instant timestamp;
        
        AuditEvent(String tradeId, String symbol, double price, int quantity,
                  String trader, String account, String event, String details,
                  Instant timestamp) {
            this.tradeId = tradeId;
            this.symbol = symbol;
            this.price = price;
            this.quantity = quantity;
            this.trader = trader;
            this.account = account;
            this.event = event;
            this.details = details;
            this.timestamp = timestamp;
        }
    }
}
