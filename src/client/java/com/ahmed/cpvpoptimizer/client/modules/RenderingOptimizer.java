package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

/**
 * MODULE 11: RENDERING OPTIMIZER
 * Provides safe, configurable culling of non-essential visual clutter (distant weather/explosion smoke)
 * without hiding gameplay-critical entities or breaking other Fabric rendering mods.
 */
public final class RenderingOptimizer extends OptimizerModule {
    private final BooleanSetting distantParticleCulling;
    private final IntSliderSetting maxParticleRenderDistance;
    private final BooleanSetting skipOffscreenArmorStandDecor;

    private int culledRenderOps = 0;

    public RenderingOptimizer() {
        super(
                ModuleId.RENDERING_OPTIMIZER,
                "Rendering Optimizer",
                "Safe distance-based particle culling and non-essential visual effect reduction.",
                ModuleCategory.PERFORMANCE,
                true
        );

        this.distantParticleCulling = registerSetting(new BooleanSetting(
                "distant_particle_culling", "Distant Particle Culling",
                "Skips rendering non-essential smoke/ambient particles beyond the configured distance.",
                true, null
        ));
        this.maxParticleRenderDistance = registerSetting(new IntSliderSetting(
                "max_particle_distance", "Max Ambient Particle Distance",
                "Maximum distance in blocks for ambient particle rendering.",
                32, 12, 64, "blocks", null
        ));
        this.skipOffscreenArmorStandDecor = registerSetting(new BooleanSetting(
                "skip_distant_decor", "Cull Distant Decorative Overlays",
                "Skips non-gameplay decorative label lookups beyond 32 blocks.",
                true, null
        ));
    }

    public boolean shouldCullAmbientParticle(double distanceSquared) {
        if (!isEnabled() || !distantParticleCulling.getValue()) return false;
        double maxDist = maxParticleRenderDistance.getValue();
        boolean cull = distanceSquared > (maxDist * maxDist);
        if (cull) culledRenderOps++;
        return cull;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        culledRenderOps = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Ambient Particle Cutoff: %d blocks | Culled Distant Visuals: %d",
                maxParticleRenderDistance.getValue(),
                culledRenderOps
        );
    }
}
