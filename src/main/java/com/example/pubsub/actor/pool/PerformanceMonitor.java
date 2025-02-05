package com.example.pubsub.actor.pool;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class PerformanceMonitor {
    private static final LongAdder messageCount = new LongAdder();
    private static final LongAdder latencySum = new LongAdder();
    private static final AtomicLong maxLatency = new AtomicLong();
    private static volatile long startTime = System.nanoTime();
    
    public static void recordMessage(long latencyNanos) {
        messageCount.increment();
        latencySum.add(latencyNanos);
        updateMaxLatency(latencyNanos);
    }
    
    private static void updateMaxLatency(long latencyNanos) {
        long current;
        while (latencyNanos > (current = maxLatency.get())) {
            maxLatency.compareAndSet(current, latencyNanos);
        }
    }
    
    public static void logMetrics() {
        long count = messageCount.sum();
        long elapsed = System.nanoTime() - startTime;
        double avgLatency = latencySum.sum() / (double)count;
        
        System.out.printf("""
            Performance Metrics:
            Messages Processed: %d
            Throughput: %.2f msgs/sec
            Average Latency: %.2f μs
            Max Latency: %.2f μs%n""",
            count,
            count / (elapsed / 1_000_000_000.0),
            avgLatency / 1000.0,
            maxLatency.get() / 1000.0);
    }
    
    public static void reset() {
        messageCount.reset();
        latencySum.reset();
        maxLatency.set(0);
        startTime = System.nanoTime();
    }
}
