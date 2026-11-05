package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.IntSliderSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class RightClickOptimizer extends OptimizerModule {
    private final BooleanSetting immediateUseDispatch;
    private final IntSliderSetting itemUseCooldownTicks;
    private final BooleanSetting skipRedundantOffhand;

    private int fastRightClicks = 0;

    public RightClickOptimizer() {
        super(
                ModuleId.RIGHT_CLICK_OPTIMIZER,
                "Right Click Optimizer",
                "Minimizes right-click item-use cooldown and removes redundant hand checks.",
                ModuleCategory.INTERACTION,
                true
        );

        this.immediateUseDispatch = registerSetting(new BooleanSetting(
                "immediate_use_dispatch", "Immediate Right-Click Processing",
                "Removes artificial client delay between right-click input and interaction dispatch.",
                true, null
        ));
        this.itemUseCooldownTicks = registerSetting(new IntSliderSetting(
                "item_use_cooldown", "Right-Click Cooldown",
                "Client cooldown ticks for general right-click interactions (0 = Fastest).",
                0, 0, 4, "ticks", null
        ));
        this.skipRedundantOffhand = registerSetting(new BooleanSetting(
                "deduplicate_hand_check", "Skip Redundant Offhand Check",
                "Skips unnecessary offhand checks when mainhand action already succeeded.",
                true, null
        ));
    }

    public boolean isImmediateUseEnabled() {
        return isEnabled() && immediateUseDispatch.getValue();
    }

    public int getItemUseCooldownTicks() {
        return itemUseCooldownTicks.getValue();
    }

    public void recordFastRightClick() {
        fastRightClicks++;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        fastRightClicks = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Use Cooldown: %d ticks | Fast Interactions: %d", itemUseCooldownTicks.getValue(), fastRightClicks);
    }
}
