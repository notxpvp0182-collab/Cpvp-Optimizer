package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 7: ANCHOR OPTIMIZER
 * Optimizes Respawn Anchor charge state inspection and block-light rebuild batching diagnostics
 * without automating charging or detonation.
 */
public final class AnchorOptimizer extends OptimizerModule {
    private final BooleanSetting fastChargeStateCheck;
    private final BooleanSetting anchorRenderOptimization;
    private final BooleanSetting anchorDiagnostics;

    private final RingBufferStats anchorEvalNanos = new RingBufferStats(100);
    private int lastObservedCharge = 0;

    public AnchorOptimizer() {
        super(
                ModuleId.ANCHOR_OPTIMIZER,
                "Anchor Optimizer",
                "Optimizes Respawn Anchor state inspection and portal particle overhead during Anchor PvP.",
                ModuleCategory.COMBAT,
                true
        );

        this.fastChargeStateCheck = registerSetting(new BooleanSetting(
                "fast_charge_check", "Fast Charge Inspection",
                "Caches targeted Respawn Anchor charge property within the current client tick.",
                true, null
        ));
        this.anchorRenderOptimization = registerSetting(new BooleanSetting(
                "anchor_render_opt", "Reduce Ambient Portal Particles",
                "Reduces non-essential ambient portal particles emitted by charged anchors.",
                true, null
        ));
        this.anchorDiagnostics = registerSetting(new BooleanSetting(
                "anchor_diagnostics", "Anchor Diagnostics",
                "Tracks Respawn Anchor interaction and state evaluation times.",
                true, null
        ));
    }

    public boolean isFastChargeStateCheckEnabled() {
        return isEnabled() && fastChargeStateCheck.getValue();
    }

    public boolean isAnchorRenderOptimizationEnabled() {
        return isEnabled() && anchorRenderOptimization.getValue();
    }

    public void recordAnchorInspection(int chargeLevel, long durationNanos) {
        if (!isEnabled()) return;
        this.lastObservedCharge = chargeLevel;
        if (anchorDiagnostics.getValue()) {
            anchorEvalNanos.record(durationNanos);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        anchorEvalNanos.clear();
        lastObservedCharge = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Target Anchor Charge: %d/4 | Inspection Overhead: %.2f us",
                lastObservedCharge,
                anchorEvalNanos.average() / 1000.0
        );
    }
}
