package com.ahmed.cpvpoptimizer;

import com.ahmed.cpvpoptimizer.common.ModuleRegistry;
import com.ahmed.cpvpoptimizer.common.NetworkProtocol;
import com.ahmed.cpvpoptimizer.server.ServerPerformanceManager;
import com.ahmed.cpvpoptimizer.server.modules.ServerPerformanceOptimizer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CPVPOptimizerCommon implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CPVP-Optimizer");
    private static boolean payloadsRegistered = false;

    @Override
    public void onInitialize() {
        registerPayloadsOnce();

        // Register the Server Performance Optimizer module in the common registry so both integrated & dedicated servers work seamlessly
        ServerPerformanceOptimizer serverPerfModule = new ServerPerformanceOptimizer();
        ModuleRegistry.getInstance().register(serverPerfModule);
        ServerPerformanceManager.getInstance().bindModule(serverPerfModule);
        ServerPerformanceManager.getInstance().registerHooks();

        ServerPlayNetworking.registerGlobalReceiver(
                NetworkProtocol.HandshakeC2SPayload.ID,
                (payload, context) -> ServerPerformanceManager.getInstance().handleClientHandshake(context.player(), payload)
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerPerformanceManager.getInstance().onPlayerDisconnect(handler.getPlayer().getUuid())
        );

        LOGGER.info("[CPVP Optimizer] Common & Server authoritative hooks initialized for Minecraft 1.21.11.");
    }

    public static synchronized void registerPayloadsOnce() {
        if (payloadsRegistered) return;
        PayloadTypeRegistry.playC2S().register(
                NetworkProtocol.HandshakeC2SPayload.ID,
                NetworkProtocol.HandshakeC2SPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                NetworkProtocol.TelemetryS2CPayload.ID,
                NetworkProtocol.TelemetryS2CPayload.CODEC
        );
        payloadsRegistered = true;
    }
}
