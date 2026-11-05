package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class CrystalOptimizer extends OptimizerModule {
    private final BooleanSetting zeroCrystalPlaceDelay;
    private final BooleanSetting renderingOptimization;
    private final BooleanSetting entityLookupCaching;
    private final IntSliderSetting maxFlameParticlesPerExplosion;

    private int fastCrystalPlaces = 0;

    public CrystalOptimizer() {
        super(
                ModuleId.CRYSTAL_OPTIMIZER,
                "Crystal Optimizer",
                "Removes client-side End Crystal placement delay and reduces crystal rendering overhead.",
                ModuleCategory.COMBAT,
                true
        );

        this.zeroCrystalPlaceDelay = registerSetting(new BooleanSetting(
                "zero_crystal_place_delay", "Instant Crystal Placement Prep",
                "Removes client item-use cooldown when placing End Crystals on Obsidian/Bedrock.",
                true, null
        ));
        this.renderingOptimization = registerSetting(new BooleanSetting(
                "rendering_optimization", "Rendering Optimization",
                "Reduces redundant matrix calculations during crystal-heavy fights.",
                true, null
        ));
        this.entityLookupCaching = registerSetting(new BooleanSetting(
                "entity_processing", "Event-Driven Crystal Tracking",
                "Avoids expensive per-tick full-world entity scans.",
                true, null
        ));
        this.maxFlameParticlesPerExplosion = registerSetting(new IntSliderSetting(
                "max_explosion_particles", "Explosion Particle Cap",
                "Caps overlapping explosion particles during crystal chains.",
                16, 4, 64, "particles", null
        ));
    }

    public boolean isZeroCrystalPlaceDelayEnabled() {
        return isEnabled() && zeroCrystalPlaceDelay.getValue();
    }

    public void recordFastCrystalPlace() {
        fastCrystalPlaces++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        fastCrystalPlaces = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Fast Crystal Placements: %d | Particle Cap: %d", fastCrystalPlaces, maxFlameParticlesPerExplosion.getValue());
    }
}
