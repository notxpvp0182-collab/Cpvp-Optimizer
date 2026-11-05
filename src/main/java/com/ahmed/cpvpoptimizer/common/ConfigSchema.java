package com.ahmed.cpvpoptimizer.common;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigSchema {
    public static final int CURRENT_CONFIG_VERSION = 2;

    public int schemaVersion = CURRENT_CONFIG_VERSION;
    public boolean masterEnabled = true;
    public boolean safeMode = false;
    public boolean uiAnimationsEnabled = true;
    public Map<String, ModuleEntry> modules = new LinkedHashMap<>();

    public static final class ModuleEntry {
        public boolean enabled = true;
        public Map<String, String> settings = new LinkedHashMap<>();
    }

    /**
     * Migrates older config schemas to CURRENT_CONFIG_VERSION and sanitizes null fields.
     */
    public boolean migrateAndValidate() {
        boolean migrated = false;
        if (schemaVersion < 1) {
            schemaVersion = 1;
            migrated = true;
        }
        if (schemaVersion == 1) {
            // Migrate v1 -> v2: ensure safeMode and uiAnimationsEnabled defaults exist
            schemaVersion = CURRENT_CONFIG_VERSION;
            migrated = true;
        }
        if (modules == null) {
            modules = new LinkedHashMap<>();
            migrated = true;
        }
        for (Map.Entry<String, ModuleEntry> entry : modules.entrySet()) {
            if (entry.getValue() == null) {
                entry.setValue(new ModuleEntry());
                migrated = true;
            } else if (entry.getValue().settings == null) {
                entry.getValue().settings = new LinkedHashMap<>();
                migrated = true;
            }
        }
        return migrated;
    }
}
