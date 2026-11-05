package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 4: RIGHT CLICK OPTIMIZER
 * Optimizes local item-use and block-interact target checks without altering vanilla item-use cooldowns
 * or repeating packets.
 */
public final class RightClickOptimizer extends OptimizerModule {
    private final BooleanSetting deduplicateHandCheck;
    private final BooleanSetting interactionDiagnostics;

    private final RingBufferStats useEvalNanos = new RingBufferStats(100);
    private int redundantHandChecksSkipped = 0;

    public RightClickOptimizer() {
        super(
                ModuleId.RIGHT_CLICK_OPTIMIZER,
                "Right Click Optimizer",
                "Reduces redundant hand/target state checks during right-click interactions.",
                ModuleCategory.INTERACTION,
                true
        );

        this.deduplicateHandCheck = registerSetting(new BooleanSetting(
                "deduplicate_hand_check", "Skip Empty Offhand Eval",
                "Skips redundant offhand target evaluation when mainhand interaction succeeds or offhand is empty.",
                true, null
        ));
        this.interactionDiagnostics = registerSetting(new BooleanSetting(
                "interaction_diagnostics", "Use-Item Profiling",
                "Measures right-click dispatch preparation overhead.",
                true, null
        ));
    }

    public void recordRightClickEvaluation(long durationNanos, boolean skippedEmptyOffhand) {
        if (!isEnabled()) return;
        if (deduplicateHandCheck.getValue() && skippedEmptyOffhand) {
            redundantHandChecksSkipped++;
        }
        if (interactionDiagnostics.getValue()) {
            useEvalNanos.record(durationNanos);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        useEvalNanos.clear();
        redundantHandChecksSkipped = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Right-Click Eval: %.2f us | Skipped Empty Hand Checks: %d",
                useEvalNanos.average() / 1000.0,
                redundantHandChecksSkipped
        );
    }
}
