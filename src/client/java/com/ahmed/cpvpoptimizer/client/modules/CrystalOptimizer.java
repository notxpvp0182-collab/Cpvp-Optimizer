package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 6: CRYSTAL OPTIMIZER
 * Optimizes End Crystal entity lookup caching and optional non-essential visual overhead during
 * crystal-heavy fights while preserving 100% vanilla placement, attack, explosion, and server rules.
 */
public final class CrystalOptimizer extends OptimizerModule {
    private final BooleanSetting renderingOptimization;
    private final BooleanSetting entityLookupCaching;
    private final BooleanSetting diagnostics;
    private final IntSliderSetting maxFlameParticlesPerExplosion;

    private final RingBufferStats crystalTickNanos = new RingBufferStats(120);
    private int nearbyCrystalCount = 0;
    private int culledExplosionParticles = 0;

    public CrystalOptimizer() {
        super(
                ModuleId.CRYSTAL_OPTIMIZER,
                "Crystal Optimizer",
                "Optimizes End Crystal entity lookups and reduces redundant visual overhead in crystal fights.",
                ModuleCategory.COMBAT,
                true
        );

        this.renderingOptimization = registerSetting(new BooleanSetting(
                "rendering_optimization", "Rendering Optimization",
                "Skips redundant inner-cube matrix recalculations when crystals are outside focal range.",
                true, null
        ));
        this.entityLookupCaching = registerSetting(new BooleanSetting(
                "entity_processing", "Entity Processing Cache",
                "Caches nearby EndCrystalEntity bounding-box scans within the same client tick.",
                true, null
        ));
        this.diagnostics = registerSetting(new BooleanSetting(
                "diagnostics", "Diagnostics",
                "Profiles client-side crystal scan & rendering overhead.",
                true, null
        ));
        this.maxFlameParticlesPerExplosion = registerSetting(new IntSliderSetting(
                "max_explosion_particles", "Explosion Particle Cap",
                "Bounds excessive overlapping explosion smoke particles during rapid crystal chains.",
                16, 4, 64, "particles", null
        ));
    }

    public boolean isRenderingOptimizationEnabled() {
        return isEnabled() && renderingOptimization.getValue();
    }

    public boolean isEntityLookupCachingEnabled() {
        return isEnabled() && entityLookupCaching.getValue();
    }

    public int getMaxFlameParticlesPerExplosion() {
        return maxFlameParticlesPerExplosion.getValue();
    }

    public void recordCrystalTickScan(int count, long durationNanos) {
        if (!isEnabled()) return;
        this.nearbyCrystalCount = count;
        if (diagnostics.getValue()) {
            crystalTickNanos.record(durationNanos);
        }
    }

    public void recordCulledParticles(int culled) {
        if (isEnabled() && culled > 0) {
            this.culledExplosionParticles += culled;
        }
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        crystalTickNanos.clear();
        nearbyCrystalCount = 0;
        culledExplosionParticles = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Active Crystals: %d | Scan Time: %.2f us | Capped Particles: %d",
                nearbyCrystalCount,
                crystalTickNanos.average() / 1000.0,
                culledExplosionParticles
        );
    }
}
