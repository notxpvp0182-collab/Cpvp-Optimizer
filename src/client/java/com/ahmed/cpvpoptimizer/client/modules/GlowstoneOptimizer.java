package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class GlowstoneOptimizer extends OptimizerModule {
    private final BooleanSetting zeroGlowstoneDelay;
    private final BooleanSetting fastAnchorTargetCheck;

    private int fastCharges = 0;

    public GlowstoneOptimizer() {
        super(
                ModuleId.GLOWSTONE_OPTIMIZER,
                "Glowstone Optimizer",
                "Removes client delay when charging Respawn Anchors with Glowstone.",
                ModuleCategory.COMBAT,
                true
        );

        this.zeroGlowstoneDelay = registerSetting(new BooleanSetting(
                "zero_glowstone_delay", "Zero Glowstone Charge Delay",
                "Eliminates the 4-tick wait when right-clicking an Anchor with Glowstone.",
                true, null
        ));
        this.fastAnchorTargetCheck = registerSetting(new BooleanSetting(
                "fast_anchor_target", "Fast Anchor Target Validation",
                "Short-circuits non-anchor block checks while holding Glowstone.",
                true, null
        ));
    }

    public boolean isZeroGlowstoneDelayEnabled() {
        return isEnabled() && zeroGlowstoneDelay.getValue();
    }

    public void recordFastCharge() {
        fastCharges++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        fastCharges = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Fast Glowstone Charges: %d | Instant Charge: ON", fastCharges);
    }
}
