package com.ahmed.cpvpoptimizer.common;

import java.util.Arrays;

/**
 * Zero-allocation bounded ring buffer for frame-time, tick-time, and RTT statistics.
 */
public final class RingBufferStats {
    private final long[] samples;
    private final long[] scratchSort;
    private int head = 0;
    private int count = 0;
    private long sum = 0L;

    public RingBufferStats(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        this.samples = new long[capacity];
        this.scratchSort = new long[capacity];
    }

    public synchronized void record(long value) {
        if (count == samples.length) {
            sum -= samples[head];
        } else {
            count++;
        }
        samples[head] = value;
        sum += value;
        head = (head + 1) % samples.length;
    }

    public synchronized void clear() {
        Arrays.fill(samples, 0L);
        head = 0;
        count = 0;
        sum = 0L;
    }

    public synchronized int size() {
        return count;
    }

    public synchronized long latest() {
        if (count == 0) return 0L;
        int idx = (head - 1 + samples.length) % samples.length;
        return samples[idx];
    }

    public synchronized double average() {
        return count == 0 ? 0.0 : (double) sum / count;
    }

    public synchronized long max() {
        long max = 0L;
        for (int i = 0; i < count; i++) {
            if (samples[i] > max) max = samples[i];
        }
        return max;
    }

    /**
     * Computes percentile (e.g., 0.99 for 99th percentile frame time / 1% low FPS).
     */
    public synchronized long percentile(double p) {
        if (count == 0) return 0L;
        System.arraycopy(samples, 0, scratchSort, 0, count);
        Arrays.sort(scratchSort, 0, count);
        int index = Math.min(count - 1, Math.max(0, (int) Math.ceil(p * count) - 1));
        return scratchSort[index];
    }
}
