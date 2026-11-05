package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 10: FPS OPTIMIZER
 * Monitors real-time FPS, frame-time stability, and 1% low FPS using a zero-allocation ring buffer.
 * Never silently alters user options.txt settings.
 */
public final class FPSOptimizer extends OptimizerModule {
    private final BooleanSetting monitorOnePercentLow;
    private final BooleanSetting throttleBackgroundParticles;
    private final IntSliderSetting frameSpikeBudgetMs;

    private final RingBufferStats frameDurationMicros = new RingBufferStats(240);
    private int currentFps = 0;

    public FPSOptimizer() {
        super(
                ModuleId.FPS_OPTIMIZER,
                "FPS Optimizer",
                "Monitors frame-time stability, 1% low FPS, and caps frame-spike particle bursts.",
                ModuleCategory.PERFORMANCE,
                true
        );

        this.monitorOnePercentLow = registerSetting(new BooleanSetting(
                "monitor_1pct_low", "Track 1% Low FPS",
                "Computes 99th-percentile frame time to diagnose micro-stutters.",
                true, null
        ));
        this.throttleBackgroundParticles = registerSetting(new BooleanSetting(
                "throttle_burst_particles", "Frame-Spike Particle Guard",
                "Temporarily throttles non-gameplay ambient particles when frame time exceeds budget.",
                true, null
        ));
        this.frameSpikeBudgetMs = registerSetting(new IntSliderSetting(
                "frame_spike_budget_ms", "Frame-Time Spike Threshold",
                "Frame duration threshold that triggers burst-particle throttling.",
                16, 8, 50, "ms", null
        ));
    }

    public void recordFrame(long frameDurationNanos, int clientFps) {
        if (!isEnabled()) return;
        this.currentFps = clientFps;
        this.frameDurationMicros.record(Math.max(1L, frameDurationNanos / 1_000L));
    }

    public long getLatestFrameMicros() {
        return frameDurationMicros.latest();
    }

    public int getOnePercentLowFps() {
        long p99Micros = frameDurationMicros.percentile(0.99);
        if (p99Micros <= 0L) return currentFps;
        return (int) Math.min(1000L, 1_000_000L / p99Micros);
    }

    public boolean shouldThrottleAmbientBurst() {
        return isEnabled()
                && throttleBackgroundParticles.getValue()
                && (frameDurationMicros.latest() / 1_000L) > frameSpikeBudgetMs.getValue();
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        frameDurationMicros.clear();
    }

    @Override
    public String getDiagnosticsSummary() {
        double avgMs = frameDurationMicros.average() / 1000.0;
        int onePctLow = monitorOnePercentLow.getValue() ? getOnePercentLowFps() : currentFps;
        return String.format(
                Locale.ROOT,
                "FPS: %d | 1%% Low: %d FPS | Avg Frame: %.2f ms",
                currentFps, onePctLow, avgMs
        );
    }
}
