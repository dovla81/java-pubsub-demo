package com.example.pubsub.monitoring;

import org.HdrHistogram.Histogram;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LatencyMonitor {
    private static final ConcurrentHashMap<String, Histogram> histograms = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private static final long HIGHEST_TRACKABLE_VALUE = 30_000_000_000L; // 30 seconds in nanos
    
    static {
        // Initialize histograms for different processing stages
        histograms.put("validation", new Histogram(HIGHEST_TRACKABLE_VALUE, 2));
        histograms.put("pricing", new Histogram(HIGHEST_TRACKABLE_VALUE, 2));
        histograms.put("risk", new Histogram(HIGHEST_TRACKABLE_VALUE, 2));
        histograms.put("execution", new Histogram(HIGHEST_TRACKABLE_VALUE, 2));
        histograms.put("total", new Histogram(HIGHEST_TRACKABLE_VALUE, 2));
        
        // Initialize counters
        counters.put("received", new AtomicLong(0));
        counters.put("processed", new AtomicLong(0));
        counters.put("rejected", new AtomicLong(0));
        counters.put("errors", new AtomicLong(0));
    }
    
    public static void recordLatency(String stage, long startNanos) {
        long latency = System.nanoTime() - startNanos;
        Histogram histogram = histograms.get(stage);
        if (histogram != null) {
            histogram.recordValue(latency);
        }
    }
    
    public static void incrementCounter(String counter) {
        AtomicLong count = counters.get(counter);
        if (count != null) {
            count.incrementAndGet();
        }
    }
    
    public static void logMetrics() {
        System.out.println("=== Latency Metrics ===");
        histograms.forEach((stage, histogram) -> {
            System.out.printf("%s Latency (μs): min=%.2f, mean=%.2f, 99%%=%.2f, max=%.2f%n",
                stage,
                histogram.getMinValue() / 1000.0,
                histogram.getMean() / 1000.0,
                histogram.getValueAtPercentile(99.0) / 1000.0,
                histogram.getMaxValue() / 1000.0);
        });
        
        System.out.println("\n=== Throughput Metrics ===");
        counters.forEach((name, counter) -> {
            System.out.printf("%s: %d%n", name, counter.get());
        });
    }
    
    public static void reset() {
        histograms.values().forEach(Histogram::reset);
        counters.values().forEach(counter -> counter.set(0));
    }
}
