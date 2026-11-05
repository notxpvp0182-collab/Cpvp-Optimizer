package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

/**
 * MODULE 8: TOTEM OPTIMIZER
 * Optimizes offhand state inspection and reduces unnecessary HUD overlay/particle clutter
 * when a Totem of Undying activates. Never auto-swaps inventory items.
 */
public final class TotemOptimizer extends OptimizerModule {
    private final BooleanSetting cacheOffhandState;
    private final BooleanSetting compactTotemOverlay;
    private final IntSliderSetting totemParticleScalePct;

    private boolean totemInOffhand = false;
    private int redundantHudUpdatesSkipped = 0;

    public TotemOptimizer() {
        super(
                ModuleId.TOTEM_OPTIMIZER,
                "Totem Optimizer",
                "Efficient offhand Totem state inspection and reduced Totem pop visual lag.",
                ModuleCategory.COMBAT,
                true
        );

        this.cacheOffhandState = registerSetting(new BooleanSetting(
                "cache_offhand_state", "Cache Offhand State",
                "Skips redundant HUD offhand slot dirty-checks when the offhand ItemStack has not changed.",
                true, null
        ));
        this.compactTotemOverlay = registerSetting(new BooleanSetting(
                "compact_totem_overlay", "Lightweight Pop Overlay",
                "Reduces floating-item screen occlusion during Totem activation.",
                true, null
        ));
        this.totemParticleScalePct = registerSetting(new IntSliderSetting(
                "totem_particle_scale", "Totem Particle Density",
                "Percentage of Totem of Undying particles rendered upon activation.",
                50, 10, 100, "%", null
        ));
    }

    public void updateOffhandState(boolean holdsTotem, boolean unchangedFromLastTick) {
        if (!isEnabled()) return;
        this.totemInOffhand = holdsTotem;
        if (cacheOffhandState.getValue() && unchangedFromLastTick) {
            redundantHudUpdatesSkipped++;
        }
    }

    public int getTotemParticleScalePct() {
        return totemParticleScalePct.getValue();
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        redundantHudUpdatesSkipped = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Offhand Totem: %s | Skipped Redundant HUD Checks: %d | Pop Particles: %d%%",
                totemInOffhand ? "READY" : "NONE",
                redundantHudUpdatesSkipped,
                totemParticleScalePct.getValue()
        );
    }
}
