package com.ahmed.cpvpoptimizer.server.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.server.ServerPerformanceManager;

import java.util.Locale;

/**
 * MODULE 12: SERVER PERFORMANCE OPTIMIZER
 * Monitors server MSPT / tick duration and provides bounded event-processing diagnostics.
 * Never alters vanilla tick rate or falsifies TPS.
 */
public final class ServerPerformanceOptimizer extends OptimizerModule {
    private final BooleanSetting tickProfiling;
    private final BooleanSetting deduplicateDiagnosticLookups;
    private final IntSliderSetting telemetryIntervalSec;

    public ServerPerformanceOptimizer() {
        super(
                ModuleId.SERVER_PERFORMANCE_OPTIMIZER,
                "Server Performance Optimizer",
                "Monitors server tick duration (MSPT) and optimizes diagnostic event tracking on the server.",
                ModuleCategory.SERVER,
                true
        );

        this.tickProfiling = registerSetting(new BooleanSetting(
                "tick_profiling",
                "Tick Duration Profiling",
                "Measures authoritative server tick times in microseconds.",
                true,
                null
        ));

        this.deduplicateDiagnosticLookups = registerSetting(new BooleanSetting(
                "deduplicate_lookups",
                "Bounded Event Deduplication",
                "Avoids repeated per-tick diagnostic lookups on the same block position.",
                true,
                null
        ));

        this.telemetryIntervalSec = registerSetting(new IntSliderSetting(
                "telemetry_interval_sec",
                "Telemetry Sync Interval",
                "Minimum interval between server performance telemetry snapshots.",
                2, 1, 10, "s",
                null
        ));
    }

    public boolean isTickProfilingEnabled() {
        return isEnabled() && tickProfiling.getValue();
    }

    public boolean isDeduplicateLookupsEnabled() {
        return isEnabled() && deduplicateDiagnosticLookups.getValue();
    }

    public int getTelemetryIntervalSec() {
        return telemetryIntervalSec.getValue();
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {}

    @Override
    public String getDiagnosticsSummary() {
        ServerPerformanceManager mgr = ServerPerformanceManager.getInstance();
        if (!mgr.isServerComponentActive()) {
            return "Server Component Unavailable on Remote Host";
        }
        return String.format(
                Locale.ROOT,
                "Server MSPT: %.2f ms (Max: %.2f ms) | Crystal/Anchor/Place (10s): %d/%d/%d",
                mgr.getDiagnostics().getEstimatedMspt(),
                mgr.getDiagnostics().getMaxTickMicros() / 1000.0,
                mgr.getDiagnostics().getCrystalEventsCount(),
                mgr.getDiagnostics().getAnchorEventsCount(),
                mgr.getDiagnostics().getPlacementEventsCount()
        );
    }
}
