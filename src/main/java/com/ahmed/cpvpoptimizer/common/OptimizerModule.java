package com.ahmed.cpvpoptimizer.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class OptimizerModule {
    private final String id;
    private final String displayName;
    private final String description;
    private final ModuleCategory category;
    private final boolean defaultEnabled;
    private boolean enabled;
    private final List<ModuleSetting<?>> settings = new ArrayList<>();
    private final ModuleSetting.TextSetting customTagSetting;

    protected OptimizerModule(String id, String displayName, String description, ModuleCategory category, boolean defaultEnabled) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
        this.customTagSetting = new ModuleSetting.TextSetting(
                "custom_tag",
                "Custom Label",
                "Type a custom mode label or note for this module",
                "Fast",
                18,
                null
        );
        this.settings.add(this.customTagSetting);
    }

    protected <T extends ModuleSetting<?>> T registerSetting(T setting) {
        this.settings.add(setting);
        return setting;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public ModuleCategory getCategory() { return category; }
    public boolean isDefaultEnabled() { return defaultEnabled; }
    public boolean isEnabled() { return enabled; }
    public String getCustomTag() { return customTagSetting.getValue(); }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public List<ModuleSetting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void resetDefaults() {
        for (ModuleSetting<?> setting : settings) {
            setting.reset();
        }
        setEnabled(defaultEnabled);
    }

    protected abstract void onEnable();
    protected abstract void onDisable();
    public abstract String getDiagnosticsSummary();
}
