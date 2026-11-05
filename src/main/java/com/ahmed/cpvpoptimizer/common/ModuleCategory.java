package com.ahmed.cpvpoptimizer.common;

public enum ModuleCategory {
    COMBAT("Combat"),
    INTERACTION("Interaction"),
    PERFORMANCE("Performance"),
    NETWORK("Network"),
    SERVER("Server");

    private final String displayName;

    ModuleCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
