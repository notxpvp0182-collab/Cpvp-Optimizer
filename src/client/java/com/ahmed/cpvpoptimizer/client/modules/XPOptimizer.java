package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class XPOptimizer extends OptimizerModule {
    private final BooleanSetting fastXpThrow;
    private final IntSliderSetting xpUseDelayTicks;
    private final BooleanSetting optimizeOrbLookup;

    private int fastThrowsProcessed = 0;

    public XPOptimizer() {
        super(
                ModuleId.XP_OPTIMIZER,
                "XP Optimizer",
                "Removes client-side XP bottle use delay and optimizes experience orb processing.",
                ModuleCategory.COMBAT,
                true
        );

        this.fastXpThrow = registerSetting(new BooleanSetting(
                "fast_xp_throw", "Fast XP Bottle Interaction",
                "Eliminates avoidable client-side right-click cooldown while holding Experience Bottles.",
                true, null
        ));
        this.xpUseDelayTicks = registerSetting(new IntSliderSetting(
                "xp_use_delay", "XP Use Cooldown",
                "Client item-use cooldown ticks when throwing XP bottles (0 = Immediate).",
                0, 0, 4, "ticks", null
        ));
        this.optimizeOrbLookup = registerSetting(new BooleanSetting(
                "optimize_orb_lookup", "Lightweight XP Orb Processing",
                "Avoids redundant client-side XP orb entity scans.",
                true, null
        ));
    }

    public boolean isFastXpThrowEnabled() {
        return isEnabled() && fastXpThrow.getValue();
    }

    public int getXpUseDelayTicks() {
        return xpUseDelayTicks.getValue();
    }

    public void recordFastThrow() {
        fastThrowsProcessed++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        fastThrowsProcessed = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "XP Throw Delay: %d ticks | Fast Throws: %d", xpUseDelayTicks.getValue(), fastThrowsProcessed);
    }
}
