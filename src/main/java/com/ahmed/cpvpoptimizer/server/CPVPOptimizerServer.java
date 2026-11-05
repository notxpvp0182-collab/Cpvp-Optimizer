package com.ahmed.cpvpoptimizer.server;

import com.ahmed.cpvpoptimizer.CPVPOptimizerCommon;
import net.fabricmc.api.DedicatedServerModInitializer;

public final class CPVPOptimizerServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        ServerPerformanceManager.getInstance().setRemoteServerActive(true);
        CPVPOptimizerCommon.LOGGER.info(
                "[CPVP Optimizer] Dedicated Server initialized. Authoritative tick & CPVP diagnostics active."
        );
    }
}
