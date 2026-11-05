package com.ahmed.cpvpoptimizer.server;

import com.ahmed.cpvpoptimizer.common.NetworkProtocol;
import com.ahmed.cpvpoptimizer.server.modules.ServerPerformanceOptimizer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerPerformanceManager {
    private static final ServerPerformanceManager INSTANCE = new ServerPerformanceManager();

    private final ServerDiagnostics diagnostics = new ServerDiagnostics();
    private final Map<UUID, Long> lastPlayerRequestTimeMs = new ConcurrentHashMap<>();
    private ServerPerformanceOptimizer module;
    private long tickStartNanos = 0L;
    private long tickCounter = 0L;
    private volatile boolean remoteServerActive = false;

    private ServerPerformanceManager() {}

    public static ServerPerformanceManager getInstance() {
        return INSTANCE;
    }

    public void bindModule(ServerPerformanceOptimizer module) {
        this.module = module;
    }

    public ServerDiagnostics getDiagnostics() {
        return diagnostics;
    }

    public boolean isServerComponentActive() {
        return remoteServerActive || tickCounter > 0L;
    }

    public void setRemoteServerActive(boolean active) {
        this.remoteServerActive = active;
    }

    public void applyRemoteTelemetry(NetworkProtocol.TelemetryS2CPayload payload) {
        this.remoteServerActive = true;
        this.diagnostics.recordTickDurationNanos(payload.avgTickMicros() * 1_000L);
    }

    public void registerHooks() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            tickStartNanos = System.nanoTime();
            diagnostics.onServerTickStart(tickCounter);
            diagnostics.resetWindowCountersIfNeeded(tickCounter);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (module == null || module.isTickProfilingEnabled()) {
                long elapsed = System.nanoTime() - tickStartNanos;
                diagnostics.recordTickDurationNanos(elapsed);
            }
        });

        // Observe Crystal attacks without altering vanilla damage, reach, or cooldown validation
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && entity instanceof EndCrystalEntity) {
                diagnostics.recordCrystalInteraction();
            }
            return ActionResult.PASS;
        });

        // Observe Crystal placement, Respawn Anchor interactions, and Block Placements
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClient()) {
                if (module == null || !module.isDeduplicateLookupsEnabled()
                        || diagnostics.markBlockPosProfiledThisTick(hitResult.getBlockPos())) {
                    ItemStack held = player.getStackInHand(hand);
                    if (held.isOf(Items.END_CRYSTAL)) {
                        diagnostics.recordCrystalInteraction();
                    } else if (held.isOf(Items.GLOWSTONE)
                            || world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.RESPAWN_ANCHOR)) {
                        diagnostics.recordAnchorInteraction();
                    } else if (held.getItem() instanceof BlockItem) {
                        diagnostics.recordBlockPlacement();
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    public void handleClientHandshake(ServerPlayerEntity player, NetworkProtocol.HandshakeC2SPayload payload) {
        if (player == null || payload == null) return;

        // Enforce rate limit per player
        long now = System.currentTimeMillis();
        Long lastRequest = lastPlayerRequestTimeMs.get(player.getUuid());
        if (lastRequest != null && (now - lastRequest) < NetworkProtocol.MIN_SYNC_INTERVAL_MS) {
            return;
        }
        lastPlayerRequestTimeMs.put(player.getUuid(), now);

        int negotiatedCapabilities = NetworkProtocol.isVersionCompatible(payload.protocolVersion())
                ? (payload.requestedCapabilities() & NetworkProtocol.ALL_SERVER_CAPABILITIES)
                : 0;

        NetworkProtocol.TelemetryS2CPayload response = new NetworkProtocol.TelemetryS2CPayload(
                NetworkProtocol.PROTOCOL_VERSION,
                negotiatedCapabilities,
                payload.clientTimestampNanos(),
                diagnostics.getAvgTickMicros(),
                diagnostics.getMaxTickMicros(),
                diagnostics.getCrystalEventsCount(),
                diagnostics.getAnchorEventsCount(),
                diagnostics.getPlacementEventsCount()
        );

        ServerPlayNetworking.send(player, response);
    }

    public void onPlayerDisconnect(UUID uuid) {
        lastPlayerRequestTimeMs.remove(uuid);
    }
}
