package com.example.pubsub.latency;

import com.example.pubsub.model.MarketTrade;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class RingBuffer {
    private static final Unsafe UNSAFE;
    private final int capacity;
    private final int mask;
    private final AtomicReferenceArray<MarketTrade> buffer;
    private final AtomicLongArray sequences;
    private final long waitSpinCount;
    
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public RingBuffer(int capacity) {
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;
        this.buffer = new AtomicReferenceArray<>(this.capacity);
        this.sequences = new AtomicLongArray(this.capacity);
        this.waitSpinCount = 1000; // Configurable spin wait
        
        // Initialize sequences
        for (int i = 0; i < this.capacity; i++) {
            sequences.set(i, i);
        }
    }
    
    public boolean offer(MarketTrade trade, long sequence) {
        final int index = (int) (sequence & mask);
        
        // Spin until slot is available
        for (long i = 0; i < waitSpinCount; i++) {
            if (sequences.get(index) == sequence - capacity) {
                buffer.set(index, trade);
                sequences.set(index, sequence);
                return true;
            }
            UNSAFE.loadFence(); // Memory barrier
        }
        return false;
    }
    
    public MarketTrade poll(long sequence) {
        final int index = (int) (sequence & mask);
        
        // Spin until data is available
        for (long i = 0; i < waitSpinCount; i++) {
            if (sequences.get(index) == sequence) {
                MarketTrade trade = buffer.get(index);
                sequences.set(index, sequence + capacity);
                return trade;
            }
            UNSAFE.loadFence(); // Memory barrier
        }
        return null;
    }
    
    private static int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }
    
    public int capacity() {
        return capacity;
    }
}
