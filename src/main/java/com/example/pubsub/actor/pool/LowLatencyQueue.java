package com.example.pubsub.actor.pool;

import com.example.pubsub.Trade;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicLong;

public class LowLatencyQueue {
    private final AtomicReferenceArray<Trade> buffer;
    private final int mask;
    private final AtomicLong head = new AtomicLong(0);
    private final AtomicLong tail = new AtomicLong(0);
    
    public LowLatencyQueue(int size) {
        // Must be power of 2 for efficient masking
        int actualSize = nextPowerOfTwo(size);
        this.buffer = new AtomicReferenceArray<>(actualSize);
        this.mask = actualSize - 1;
    }
    
    public boolean offer(Trade trade) {
        final long currentTail = tail.get();
        final long wrapPoint = currentTail - buffer.length();
        if (head.get() <= wrapPoint) {
            return false; // Queue is full
        }
        
        buffer.set((int)(currentTail & mask), trade);
        tail.lazySet(currentTail + 1); // Use lazySet for better performance
        return true;
    }
    
    public Trade poll() {
        final long currentHead = head.get();
        if (currentHead >= tail.get()) {
            return null; // Queue is empty
        }
        
        Trade trade = buffer.get((int)(currentHead & mask));
        head.lazySet(currentHead + 1);
        return trade;
    }
    
    private static int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
}
