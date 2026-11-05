package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;

import java.util.Locale;

public final class HotbarOptimizer extends OptimizerModule {
    private final BooleanSetting instantSlotSync;
    private final BooleanSetting resetUseDelayOnSwitch;

    private int lastSyncedSlot = -1;
    private int instantSwitchesProcessed = 0;

    public HotbarOptimizer() {
        super(
                ModuleId.HOTBAR_OPTIMIZER,
                "Hotbar Optimizer",
                "Eliminates hotbar slot switch delay and immediately synchronizes selected slot before interaction.",
                ModuleCategory.INTERACTION,
                true
        );

        this.instantSlotSync = registerSetting(new BooleanSetting(
                "instant_slot_sync", "Immediate Slot Synchronization",
                "Immediately synchronizes newly selected hotbar slot without waiting for end-of-tick.",
                true, null
        ));
        this.resetUseDelayOnSwitch = registerSetting(new BooleanSetting(
                "reset_delay_on_switch", "Clear Interaction Delay on Switch",
                "Clears residual right-click cooldown when switching hotbar slots (Anchor -> Glowstone -> Totem).",
                true, null
        ));
    }

    public boolean isInstantSlotSyncEnabled() {
        return isEnabled() && instantSlotSync.getValue();
    }

    public boolean shouldResetUseDelayOnSwitch() {
        return isEnabled() && resetUseDelayOnSwitch.getValue();
    }

    public boolean checkSlotChanged(int currentSlot) {
        if (!isEnabled()) return false;
        if (currentSlot != lastSyncedSlot) {
            lastSyncedSlot = currentSlot;
            instantSwitchesProcessed++;
            return true;
        }
        return false;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        lastSyncedSlot = -1;
        instantSwitchesProcessed = 0;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(Locale.ROOT, "Active Slot: %d | Instant Switches: %d", lastSyncedSlot + 1, instantSwitchesProcessed);
    }
}
