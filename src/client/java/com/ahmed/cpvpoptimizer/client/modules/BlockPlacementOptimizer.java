package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

public final class BlockPlacementOptimizer extends OptimizerModule {
    private final BooleanSetting zeroPlaceDelay;
    private final IntSliderSetting placementCooldownTicks;
    private final BooleanSetting cachePlacementRaycast;

    private long lastCheckedTick = -1L;
    private long lastPackedBlockPos = 0L;
    private int fastPlacements = 0;

    public BlockPlacementOptimizer() {
        super(
                ModuleId.BLOCK_PLACEMENT_OPTIMIZER,
                "Block Placement Optimizer",
                "Eliminates client-side block placement delay and caches target raycasts.",
                ModuleCategory.INTERACTION,
                true
        );

        this.zeroPlaceDelay = registerSetting(new BooleanSetting(
                "zero_place_delay", "Remove Block Place Delay",
                "Removes the 4-tick client wait between consecutive block placements (Obsidian/Anchors).",
                true, null
        ));
        this.placementCooldownTicks = registerSetting(new IntSliderSetting(
                "placement_cooldown", "Placement Cooldown",
                "Client cooldown ticks between block placements (0 = Instant).",
                0, 0, 4, "ticks", null
        ));
        this.cachePlacementRaycast = registerSetting(new BooleanSetting(
                "cache_placement_raycast", "Same-Tick Raycast Cache",
                "Avoids recomputing identical block placement raycasts when position/rotation is unchanged.",
                true, null
        ));
    }

    public boolean isZeroPlaceDelayEnabled() {
        return isEnabled() && zeroPlaceDelay.getValue();
    }

    public int getPlacementCooldownTicks() {
        return placementCooldownTicks.getValue();
    }

    public void recordFastPlacement() {
        fastPlacements++;
    }

    public boolean checkAndRecordTarget(long clientTick, BlockPos pos) {
        if (!isEnabled() || !cachePlacementRaycast.getValue() || pos == null) return false;
        long packed = pos.asLong();
        boolean hit = (clientTick == lastCheckedTick && packed == lastPackedBlockPos);
        lastCheckedTick = clientTick;
        lastPackedBlockPos = packed;
        return hit;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        fastPlacements = 0;
        lastCheckedTick = -1L;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Place Delay: %d ticks | Fast Placements: %d", placementCooldownTicks.getValue(), fastPlacements);
    }
}
