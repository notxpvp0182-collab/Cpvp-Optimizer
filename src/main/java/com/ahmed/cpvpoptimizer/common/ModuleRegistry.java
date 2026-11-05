package com.ahmed.cpvpoptimizer.common;

import java.util.*;

public final class ModuleRegistry {
    private static final ModuleRegistry INSTANCE = new ModuleRegistry();

    private final Map<String, OptimizerModule> modulesById = new LinkedHashMap<>();

    private ModuleRegistry() {}

    public static ModuleRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void register(OptimizerModule module) {
        Objects.requireNonNull(module, "module cannot be null");
        modulesById.put(module.getId(), module);
    }

    public synchronized Optional<OptimizerModule> getById(String id) {
        return Optional.ofNullable(modulesById.get(id));
    }

    public synchronized List<OptimizerModule> getAllModules() {
        return List.copyOf(modulesById.values());
    }

    public synchronized List<OptimizerModule> getByCategory(ModuleCategory category) {
        List<OptimizerModule> result = new ArrayList<>();
        for (OptimizerModule module : modulesById.values()) {
            if (module.getCategory() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public synchronized void disableAllForSafeMode() {
        for (OptimizerModule module : modulesById.values()) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    public synchronized void clearForTesting() {
        modulesById.clear();
    }
}
