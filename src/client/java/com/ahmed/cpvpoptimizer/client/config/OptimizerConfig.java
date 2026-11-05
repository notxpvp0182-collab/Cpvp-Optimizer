package com.ahmed.cpvpoptimizer.client.config;

import com.ahmed.cpvpoptimizer.CPVPOptimizerCommon;
import com.ahmed.cpvpoptimizer.common.ConfigSchema;
import com.ahmed.cpvpoptimizer.common.ModuleRegistry;
import com.ahmed.cpvpoptimizer.common.ModuleSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class OptimizerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final OptimizerConfig INSTANCE = new OptimizerConfig();

    private Path configPath;
    private ConfigSchema schema = new ConfigSchema();

    private OptimizerConfig() {
        try {
            this.configPath = FabricLoader.getInstance().getConfigDir().resolve("cpvpoptimizer.json");
        } catch (Throwable ignored) {
            this.configPath = Path.of("build", "tmp", "cpvpoptimizer-test.json");
        }
    }

    public static OptimizerConfig getInstance() {
        return INSTANCE;
    }

    public void setConfigPathForTesting(Path customPath) {
        this.configPath = customPath;
    }

    public boolean isMasterEnabled() {
        return schema.masterEnabled && !schema.safeMode;
    }

    public void setMasterEnabled(boolean enabled) {
        this.schema.masterEnabled = enabled;
        save();
    }

    public boolean isSafeMode() {
        return schema.safeMode;
    }

    public void setSafeMode(boolean safeMode) {
        this.schema.safeMode = safeMode;
        if (safeMode) {
            ModuleRegistry.getInstance().disableAllForSafeMode();
        }
        save();
    }

    public boolean isUiAnimationsEnabled() {
        return schema.uiAnimationsEnabled;
    }

    public void setUiAnimationsEnabled(boolean enabled) {
        this.schema.uiAnimationsEnabled = enabled;
        save();
    }

    /**
     * Activates the "Maximum Responsiveness" profile:
     * Enables all modules and sets all interaction cooldowns to 0 ticks.
     */
    public synchronized void applyMaximumResponsivenessProfile() {
        this.schema.masterEnabled = true;
        this.schema.safeMode = false;
        for (OptimizerModule module : ModuleRegistry.getInstance().getAllModules()) {
            module.resetDefaults();
            module.setEnabled(true);
            for (ModuleSetting<?> setting : module.getSettings()) {
                if (setting instanceof ModuleSetting.IntSliderSetting slider && slider.getMin() == 0) {
                    slider.setValue(0);
                } else if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                    boolSetting.setValue(true);
                }
            }
        }
        save();
    }

    public synchronized void load() {
        if (!Files.exists(configPath)) {
            syncFromRegistryToSchema();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ConfigSchema loaded = GSON.fromJson(reader, ConfigSchema.class);
            if (loaded == null) {
                loaded = new ConfigSchema();
            }
            boolean migrated = loaded.migrateAndValidate();
            this.schema = loaded;
            applySchemaToRegistry();
            if (migrated) {
                save();
            }
        } catch (Exception e) {
            CPVPOptimizerCommon.LOGGER.warn("[CPVP Optimizer] Corrupted config detected; restoring safe defaults.", e);
            this.schema = new ConfigSchema();
            syncFromRegistryToSchema();
            save();
        }
    }

    public synchronized void save() {
        syncFromRegistryToSchema();
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tempFile = configPath.resolveSibling(configPath.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                GSON.toJson(this.schema, writer);
            }
            Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            CPVPOptimizerCommon.LOGGER.error("[CPVP Optimizer] Failed to save configuration atomically.", e);
        }
    }

    public synchronized void resetAllToDefaults() {
        this.schema.masterEnabled = true;
        this.schema.safeMode = false;
        this.schema.uiAnimationsEnabled = true;
        for (OptimizerModule module : ModuleRegistry.getInstance().getAllModules()) {
            module.resetDefaults();
        }
        save();
    }

    private void syncFromRegistryToSchema() {
        schema.schemaVersion = ConfigSchema.CURRENT_CONFIG_VERSION;
        for (OptimizerModule module : ModuleRegistry.getInstance().getAllModules()) {
            ConfigSchema.ModuleEntry entry = schema.modules.computeIfAbsent(
                    module.getId(), k -> new ConfigSchema.ModuleEntry()
            );
            entry.enabled = module.isEnabled();
            entry.settings.clear();
            for (ModuleSetting<?> setting : module.getSettings()) {
                entry.settings.put(setting.getKey(), String.valueOf(setting.getValue()));
            }
        }
    }

    private void applySchemaToRegistry() {
        for (OptimizerModule module : ModuleRegistry.getInstance().getAllModules()) {
            ConfigSchema.ModuleEntry entry = schema.modules.get(module.getId());
            if (entry == null) continue;
            module.setEnabled(!schema.safeMode && entry.enabled);
            for (ModuleSetting<?> setting : module.getSettings()) {
                String raw = entry.settings.get(setting.getKey());
                if (raw == null) continue;
                applyRawSettingValue(setting, raw);
            }
        }
    }

    private static void applyRawSettingValue(ModuleSetting<?> setting, String raw) {
        try {
            if (setting instanceof ModuleSetting.BooleanSetting boolSetting) {
                boolSetting.setValue(Boolean.parseBoolean(raw));
            } else if (setting instanceof ModuleSetting.IntSliderSetting intSetting) {
                intSetting.setValue(Integer.parseInt(raw));
            } else if (setting instanceof ModuleSetting.CycleSetting cycleSetting) {
                cycleSetting.setValue(raw);
            }
        } catch (NumberFormatException ignored) {
            setting.reset();
        }
    }
}
