package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class AnchorOptimizer extends OptimizerModule {
    private final BooleanSetting instantAnchorInteract;
    private final BooleanSetting fastChargeStateCheck;
    private final BooleanSetting anchorRenderOptimization;

    private int lastObservedCharge = 0;
    private int fastAnchorActions = 0;

    public AnchorOptimizer() {
        super(
                ModuleId.ANCHOR_OPTIMIZER,
                "Anchor Optimizer",
                "Eliminates Respawn Anchor placement, charging, and detonation interaction delays.",
                ModuleCategory.COMBAT,
                true
        );

        this.instantAnchorInteract = registerSetting(new BooleanSetting(
                "instant_anchor_interact", "Zero Anchor Interaction Delay",
                "Clears client item-use delay when placing, charging, or detonating a Respawn Anchor.",
                true, null
        ));
        this.fastChargeStateCheck = registerSetting(new BooleanSetting(
                "fast_charge_check", "Cached Charge State Lookup",
                "Caches targeted Respawn Anchor charge state until block state changes.",
                true, null
        ));
        this.anchorRenderOptimization = registerSetting(new BooleanSetting(
                "anchor_render_opt", "Reduce Ambient Portal Particles",
                "Reduces non-essential ambient portal particles around charged anchors.",
                true, null
        ));
    }

    public boolean isInstantAnchorInteractEnabled() {
        return isEnabled() && instantAnchorInteract.getValue();
    }

    public void recordAnchorAction(int charge) {
        this.lastObservedCharge = charge;
        this.fastAnchorActions++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        lastObservedCharge = 0;
        fastAnchorActions = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Anchor Charge: %d/4 | Fast Anchor Actions: %d", lastObservedCharge, fastAnchorActions);
    }
}
