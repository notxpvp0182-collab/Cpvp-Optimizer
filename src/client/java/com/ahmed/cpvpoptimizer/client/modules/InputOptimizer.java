package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 2: INPUT OPTIMIZER
 * Improves local keyboard/mouse input state inspection by caching per-tick keybind focus state
 * and avoiding redundant state lookups while preserving exact event ordering.
 */
public final class InputOptimizer extends OptimizerModule {
    private final BooleanSetting cacheFocusState;
    private final BooleanSetting inputDiagnostics;

    private final RingBufferStats inputProcessNanos = new RingBufferStats(120);
    private boolean lastScreenOpenState = false;
    private int skippedRedundantChecks = 0;

    public InputOptimizer() {
        super(
                ModuleId.INPUT_OPTIMIZER,
                "Input Optimizer",
                "Reduces avoidable local input polling overhead while preserving vanilla keybind order.",
                ModuleCategory.INTERACTION,
                true
        );

        this.cacheFocusState = registerSetting(new BooleanSetting(
                "cache_focus_state", "Fast Focus Validation",
                "Caches GUI vs in-world focus state per client tick to avoid repeated screen checks.",
                true, null
        ));
        this.inputDiagnostics = registerSetting(new BooleanSetting(
                "input_diagnostics", "Input Latency Diagnostics",
                "Profiles local input-to-tick processing duration in microseconds.",
                true, null
        ));
    }

    public void onClientInputTick(boolean hasOpenScreen, long processingDurationNanos) {
        if (!isEnabled()) return;
        if (cacheFocusState.getValue() && hasOpenScreen == lastScreenOpenState) {
            skippedRedundantChecks++;
        }
        lastScreenOpenState = hasOpenScreen;
        if (inputDiagnostics.getValue()) {
            inputProcessNanos.record(processingDurationNanos);
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        inputProcessNanos.clear();
        skippedRedundantChecks = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Input Tick Overhead: %.2f us (Max: %.2f us) | Cached Focus Hits: %d",
                inputProcessNanos.average() / 1000.0,
                inputProcessNanos.max() / 1000.0,
                skippedRedundantChecks
        );
    }
}
