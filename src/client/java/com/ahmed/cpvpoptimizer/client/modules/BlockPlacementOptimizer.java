package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

/**
 * MODULE 5: BLOCK PLACEMENT OPTIMIZER
 * Avoids redundant client-side block collision/raycast recalculations within the same tick
 * while preserving block reach, collision rules, and server validation.
 */
public final class BlockPlacementOptimizer extends OptimizerModule {
    private final BooleanSetting cachePlacementRaycast;
    private final BooleanSetting placementDiagnostics;

    private final RingBufferStats placementPrepNanos = new RingBufferStats(100);
    private long lastCheckedTick = -1L;
    private long lastPackedBlockPos = 0L;
    private int cachedRaycastHits = 0;

    public BlockPlacementOptimizer() {
        super(
                ModuleId.BLOCK_PLACEMENT_OPTIMIZER,
                "Block Placement Optimizer",
                "Caches same-tick placement target lookups and profiles obsidian/block placement.",
                ModuleCategory.INTERACTION,
                true
        );

        this.cachePlacementRaycast = registerSetting(new BooleanSetting(
                "cache_placement_raycast", "Same-Tick Raycast Cache",
                "Avoids recomputing identical block placement raycasts within the same client tick.",
                true, null
        ));
        this.placementDiagnostics = registerSetting(new BooleanSetting(
                "placement_diagnostics", "Placement Diagnostics",
                "Tracks local block placement preparation timing.",
                true, null
        ));
    }

    public boolean checkAndRecordTarget(long clientTick, BlockPos pos, long prepNanos) {
        if (!isEnabled()) return false;
        if (placementDiagnostics.getValue()) {
            placementPrepNanos.record(prepNanos);
        }
        if (!cachePlacementRaycast.getValue() || pos == null) return false;

        long packed = pos.asLong();
        boolean cacheHit = (clientTick == lastCheckedTick && packed == lastPackedBlockPos);
        if (cacheHit) {
            cachedRaycastHits++;
        } else {
            lastCheckedTick = clientTick;
            lastPackedBlockPos = packed;
        }
        return cacheHit;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        placementPrepNanos.clear();
        cachedRaycastHits = 0;
        lastCheckedTick = -1L;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Placement Prep: %.2f us | Same-Tick Raycast Cache Hits: %d",
                placementPrepNanos.average() / 1000.0,
                cachedRaycastHits
        );
    }
}
