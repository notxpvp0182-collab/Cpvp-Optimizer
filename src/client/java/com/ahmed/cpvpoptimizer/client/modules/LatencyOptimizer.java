package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.CycleSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.List;
import java.util.Locale;

/**
 * MODULE 1: LATENCY OPTIMIZER
 * Measures reliable round-trip latency, separates network RTT from server MSPT and client frame time,
 * and detects latency spikes without packet spamming or spoofing.
 */
public final class LatencyOptimizer extends OptimizerModule {
    private final BooleanSetting enableMonitoring;
    private final IntSliderSetting historyDurationSec;
    private final IntSliderSetting updateIntervalSec;
    private final CycleSetting displayStyle;
    private final IntSliderSetting spikeWarningThresholdMs;

    private final RingBufferStats rttHistoryMs = new RingBufferStats(120);
    private volatile long currentRttMs = 0L;
    private volatile long lastServerMsptMicros = 0L;
    private volatile long lastClientFrameMicros = 0L;
    private volatile boolean spikeDetected = false;
    private volatile boolean serverConnected = false;

    public LatencyOptimizer() {
        super(
                ModuleId.LATENCY_OPTIMIZER,
                "Latency Optimizer",
                "Accurate RTT diagnostics separating network latency from server MSPT and client frame time.",
                ModuleCategory.NETWORK,
                true
        );

        this.enableMonitoring = registerSetting(new BooleanSetting(
                "enable_monitoring", "Latency Monitoring", "Passively tracks player list & handshake RTT.", true, null
        ));
        this.historyDurationSec = registerSetting(new IntSliderSetting(
                "history_duration", "History Window", "Duration of rolling ping history.", 60, 15, 120, "s", null
        ));
        this.updateIntervalSec = registerSetting(new IntSliderSetting(
                "update_interval", "Update Interval", "Sampling interval in seconds.", 2, 1, 10, "s", null
        ));
        this.displayStyle = registerSetting(new CycleSetting(
                "display_style", "Display Style", "Format used for latency diagnostics.",
                "Detailed", List.of("Compact", "Detailed", "Graph-Numeric"), null
        ));
        this.spikeWarningThresholdMs = registerSetting(new IntSliderSetting(
                "spike_threshold", "Spike Threshold", "RTT jump above average that flags a network spike.",
                45, 15, 200, "ms", null
        ));
    }

    public void recordSample(long rttMs, long serverTickMicros, long clientFrameMicros, boolean connected) {
        this.serverConnected = connected;
        if (!isEnabled() || !enableMonitoring.getValue()) return;

        this.currentRttMs = Math.max(0L, rttMs);
        this.lastServerMsptMicros = Math.max(0L, serverTickMicros);
        this.lastClientFrameMicros = Math.max(0L, clientFrameMicros);

        double avgBefore = rttHistoryMs.average();
        this.spikeDetected = rttHistoryMs.size() >= 3
                && (currentRttMs - avgBefore) >= spikeWarningThresholdMs.getValue();
        rttHistoryMs.record(this.currentRttMs);
    }

    public int getUpdateIntervalTicks() {
        return updateIntervalSec.getValue() * 20;
    }

    public long getCurrentRttMs() {
        return currentRttMs;
    }

    public double getAverageRttMs() {
        return rttHistoryMs.average();
    }

    public long getMaxRttMs() {
        return rttHistoryMs.max();
    }

    public boolean isSpikeDetected() {
        return spikeDetected;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        rttHistoryMs.clear();
        spikeDetected = false;
    }

    @Override
    public String getDiagnosticsSummary() {
        if (!serverConnected) {
            return "Status: Disconnected | Waiting for world session";
        }
        if ("Compact".equals(displayStyle.getValue())) {
            return String.format(Locale.ROOT, "RTT: %d ms (Avg %.0f ms)", currentRttMs, getAverageRttMs());
        }
        return String.format(
                Locale.ROOT,
                "RTT: %d ms (Avg: %.1f ms | Max: %d ms%s) | Server Tick: %.2f ms | Frame: %.2f ms",
                currentRttMs,
                getAverageRttMs(),
                getMaxRttMs(),
                spikeDetected ? " [SPIKE]" : "",
                lastServerMsptMicros / 1000.0,
                lastClientFrameMicros / 1000.0
        );
    }
}
