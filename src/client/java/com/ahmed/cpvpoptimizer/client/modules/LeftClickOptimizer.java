package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 3: LEFT CLICK OPTIMIZER
 * Profiles local attack input handling and reuses the current frame's crosshair target
 * when valid. Strictly preserves vanilla attack cooldowns and never synthesizes clicks.
 */
public final class LeftClickOptimizer extends OptimizerModule {
    private final BooleanSetting reuseCrosshairTarget;
    private final BooleanSetting profileAttackDelay;

    private final RingBufferStats attackEvalNanos = new RingBufferStats(100);
    private int totalLeftClicks = 0;

    public LeftClickOptimizer() {
        super(
                ModuleId.LEFT_CLICK_OPTIMIZER,
                "Left Click Optimizer",
                "Profiles attack input handling and avoids duplicate target raycasts during left-click.",
                ModuleCategory.INTERACTION,
                true
        );

        this.reuseCrosshairTarget = registerSetting(new BooleanSetting(
                "reuse_crosshair_target", "Reuse Tick Crosshair Target",
                "Reuses the already-computed crosshair HitResult for the current tick.",
                true, null
        ));
        this.profileAttackDelay = registerSetting(new BooleanSetting(
                "profile_attack_delay", "Attack Dispatch Profiling",
                "Measures local left-click evaluation overhead.",
                true, null
        ));
    }

    public boolean shouldReuseCrosshairTarget() {
        return isEnabled() && reuseCrosshairTarget.getValue();
    }

    public void recordAttackEvaluation(long durationNanos) {
        if (!isEnabled()) return;
        totalLeftClicks++;
        if (profileAttackDelay.getValue()) {
            attackEvalNanos.record(durationNanos);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        attackEvalNanos.clear();
        totalLeftClicks = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Left-Click Eval: %.2f us | Observed Attacks: %d | Vanilla Cooldown: Preserved",
                attackEvalNanos.average() / 1000.0,
                totalLeftClicks
        );
    }
}
