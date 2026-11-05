package com.ahmed.cpvpoptimizer.server;

import com.ahmed.cpvpoptimizer.common.RingBufferStats;
import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded, thread-safe server diagnostics tracker for tick duration,
 * Crystal interactions, Respawn Anchor interactions, and Block Placements.
 */
public final class ServerDiagnostics {
    private final RingBufferStats tickMicrosBuffer = new RingBufferStats(200);
    private final AtomicInteger crystalEventsWindow = new AtomicInteger();
    private final AtomicInteger anchorEventsWindow = new AtomicInteger();
    private final AtomicInteger placementEventsWindow = new AtomicInteger();

    // Bounded per-tick deduplication cache to avoid redundant state lookups within the same server tick
    private static final int MAX_TICK_CACHE_ENTRIES = 256;
    private final Map<Long, Long> blockStateLookupTickCache = new ConcurrentHashMap<>(64);
    private volatile long currentServerTick = 0L;

    public void onServerTickStart(long tickCount) {
        this.currentServerTick = tickCount;
        if (blockStateLookupTickCache.size() > MAX_TICK_CACHE_ENTRIES || (tickCount % 20L == 0L)) {
            blockStateLookupTickCache.clear();
        }
    }

    public void recordTickDurationNanos(long durationNanos) {
        tickMicrosBuffer.record(Math.max(0L, durationNanos / 1_000L));
    }

    /**
     * Checks whether a given BlockPos was already profiled in the current server tick.
     * Preserves 100% of vanilla server validation while avoiding duplicate diagnostic work.
     */
    public boolean markBlockPosProfiledThisTick(BlockPos pos) {
        if (blockStateLookupTickCache.size() >= MAX_TICK_CACHE_ENTRIES) {
            return false;
        }
        long packed = pos.asLong();
        Long previousTick = blockStateLookupTickCache.put(packed, currentServerTick);
        return previousTick == null || previousTick != currentServerTick;
    }

    public void recordCrystalInteraction() {
        crystalEventsWindow.incrementAndGet();
    }

    public void recordAnchorInteraction() {
        anchorEventsWindow.incrementAndGet();
    }

    public void recordBlockPlacement() {
        placementEventsWindow.incrementAndGet();
    }

    public int getAvgTickMicros() {
        return (int) Math.round(tickMicrosBuffer.average());
    }

    public int getMaxTickMicros() {
        return (int) tickMicrosBuffer.max();
    }

    public double getEstimatedMspt() {
        return tickMicrosBuffer.average() / 1_000.0;
    }

    public int getCrystalEventsCount() {
        return crystalEventsWindow.get();
    }

    public int getAnchorEventsCount() {
        return anchorEventsWindow.get();
    }

    public int getPlacementEventsCount() {
        return placementEventsWindow.get();
    }

    public void resetWindowCountersIfNeeded(long tickCount) {
        if (tickCount % 200L == 0L) {
            crystalEventsWindow.set(0);
            anchorEventsWindow.set(0);
            placementEventsWindow.set(0);
        }
    }
}
