package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 9: GLOWSTONE OPTIMIZER
 * Profiles and caches Glowstone-to-Respawn-Anchor interaction checks without automating item use.
 */
public final class GlowstoneOptimizer extends OptimizerModule {
    private final BooleanSetting fastAnchorTargetCheck;
    private final BooleanSetting glowstoneDiagnostics;

    private final RingBufferStats glowstoneEvalNanos = new RingBufferStats(100);
    private int glowstoneInteractionsProfiled = 0;

    public GlowstoneOptimizer() {
        super(
                ModuleId.GLOWSTONE_OPTIMIZER,
                "Glowstone Optimizer",
                "Reduces redundant local block-state checks when holding Glowstone near Respawn Anchors.",
                ModuleCategory.COMBAT,
                true
        );

        this.fastAnchorTargetCheck = registerSetting(new BooleanSetting(
                "fast_anchor_target", "Fast Anchor Target Validation",
                "Short-circuits non-anchor block property lookups when holding Glowstone.",
                true, null
        ));
        this.glowstoneDiagnostics = registerSetting(new BooleanSetting(
                "glowstone_diagnostics", "Glowstone Interaction Profiling",
                "Measures client-side Glowstone charge interaction overhead.",
                true, null
        ));
    }

    public void recordGlowstoneCheck(long durationNanos) {
        if (!isEnabled()) return;
        glowstoneInteractionsProfiled++;
        if (glowstoneDiagnostics.getValue()) {
            glowstoneEvalNanos.record(durationNanos);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        glowstoneEvalNanos.clear();
        glowstoneInteractionsProfiled = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Glowstone Eval: %.2f us | Profiled Charge Checks: %d",
                glowstoneEvalNanos.average() / 1000.0,
                glowstoneInteractionsProfiled
        );
    }
}
