package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class LeftClickOptimizer extends OptimizerModule {
    private final BooleanSetting clearMissPenalty;
    private final BooleanSetting reuseCrosshairTarget;

    private int clearedMissPenalties = 0;

    public LeftClickOptimizer() {
        super(
                ModuleId.LEFT_CLICK_OPTIMIZER,
                "Left Click Optimizer",
                "Removes client-side miss-swing delay penalty and reuses crosshair raycast targets.",
                ModuleCategory.INTERACTION,
                true
        );

        this.clearMissPenalty = registerSetting(new BooleanSetting(
                "clear_miss_penalty", "Remove Miss-Swing Delay",
                "Clears the 10-tick client attack lock when a swing misses a crystal or entity.",
                true, null
        ));
        this.reuseCrosshairTarget = registerSetting(new BooleanSetting(
                "reuse_crosshair_target", "Reuse Tick Crosshair Target",
                "Avoids duplicate raycasts during left-click dispatch.",
                true, null
        ));
    }

    public boolean shouldClearMissPenalty() {
        return isEnabled() && clearMissPenalty.getValue();
    }

    public void recordClearedMissPenalty() {
        clearedMissPenalties++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        clearedMissPenalties = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Miss Lock Cleared: %d times | Direct Target Reuse: ON", clearedMissPenalties);
    }
}
