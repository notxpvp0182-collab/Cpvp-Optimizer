package com.ahmed.cpvpoptimizer.client;

import com.ahmed.cpvpoptimizer.CPVPOptimizerCommon;
import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.client.modules.*;
import com.ahmed.cpvpoptimizer.client.ui.OptimizerScreen;
import com.ahmed.cpvpoptimizer.common.ModuleRegistry;
import com.ahmed.cpvpoptimizer.common.NetworkProtocol;
import com.ahmed.cpvpoptimizer.server.ServerPerformanceManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import org.lwjgl.glfw.GLFW;

public final class CPVPOptimizerClient implements ClientModInitializer {
    private static CPVPOptimizerClient instance;

    private LatencyOptimizer latencyOptimizer;
    private InputOptimizer inputOptimizer;
    private LeftClickOptimizer leftClickOptimizer;
    private RightClickOptimizer rightClickOptimizer;
    private BlockPlacementOptimizer blockPlacementOptimizer;
    private CrystalOptimizer crystalOptimizer;
    private AnchorOptimizer anchorOptimizer;
    private TotemOptimizer totemOptimizer;
    private GlowstoneOptimizer glowstoneOptimizer;
    private FPSOptimizer fpsOptimizer;
    private RenderingOptimizer renderingOptimizer;
    private PacketOptimizer packetOptimizer;

    private KeyBinding openGuiKeyBinding;
    private long clientTickCounter = 0L;
    private long lastFrameRenderNanos = System.nanoTime();
    private ItemStack previousOffhandStack = ItemStack.EMPTY;

    @Override
    public void onInitializeClient() {
        instance = this;
        CPVPOptimizerCommon.registerPayloadsOnce();

        registerClientModules();
        OptimizerConfig.getInstance().load();

        this.openGuiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cpvpoptimizer.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.cpvpoptimizer"
        ));

        registerNetworkHandlers();
        registerTickAndRenderHooks();

        CPVPOptimizerCommon.LOGGER.info("[CPVP Optimizer] Client initialized with all 13 modules registered.");
    }

    public static CPVPOptimizerClient getInstance() {
        return instance;
    }

    public void registerClientModules() {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        // Combat Category
        this.crystalOptimizer = new CrystalOptimizer();
        this.anchorOptimizer = new AnchorOptimizer();
        this.totemOptimizer = new TotemOptimizer();
        this.glowstoneOptimizer = new GlowstoneOptimizer();
        registry.register(crystalOptimizer);
        registry.register(anchorOptimizer);
        registry.register(totemOptimizer);
        registry.register(glowstoneOptimizer);

        // Interaction Category
        this.blockPlacementOptimizer = new BlockPlacementOptimizer();
        this.rightClickOptimizer = new RightClickOptimizer();
        this.leftClickOptimizer = new LeftClickOptimizer();
        this.inputOptimizer = new InputOptimizer();
        registry.register(blockPlacementOptimizer);
        registry.register(rightClickOptimizer);
        registry.register(leftClickOptimizer);
        registry.register(inputOptimizer);

        // Performance Category
        this.fpsOptimizer = new FPSOptimizer();
        this.renderingOptimizer = new RenderingOptimizer();
        registry.register(fpsOptimizer);
        registry.register(renderingOptimizer);

        // Network Category
        this.latencyOptimizer = new LatencyOptimizer();
        this.packetOptimizer = new PacketOptimizer();
        registry.register(latencyOptimizer);
        registry.register(packetOptimizer);
    }

    private void registerNetworkHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(
                NetworkProtocol.TelemetryS2CPayload.ID,
                (payload, context) -> {
                    boolean compatible = NetworkProtocol.isVersionCompatible(payload.protocolVersion());
                    packetOptimizer.recordTelemetryPacket(36, compatible);
                    if (compatible) {
                        ServerPerformanceManager.getInstance().applyRemoteTelemetry(payload);
                        long rttMs = Math.max(0L, (System.nanoTime() - payload.echoedClientTimestampNanos()) / 1_000_000L);
                        latencyOptimizer.recordSample(
                                rttMs,
                                payload.avgTickMicros(),
                                fpsOptimizer.getLatestFrameMicros(),
                                true
                        );
                    }
                }
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerPerformanceManager.getInstance().setRemoteServerActive(false);
            sendServerHandshakeIfPossible();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ServerPerformanceManager.getInstance().setRemoteServerActive(false);
            packetOptimizer.onDisconnect();
            latencyOptimizer.recordSample(0L, 0L, 0L, false);
        });
    }

    private void registerTickAndRenderHooks() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            clientTickCounter++;
            while (openGuiKeyBinding.wasPressed()) {
                client.setScreen(new OptimizerScreen(client.currentScreen));
            }

            if (!OptimizerConfig.getInstance().isMasterEnabled() || client.player == null || client.world == null) {
                return;
            }

            long tickStart = System.nanoTime();

            // 1. Input Optimizer
            inputOptimizer.onClientInputTick(client.currentScreen != null, System.nanoTime() - tickStart);

            // 2. Totem Optimizer (Offhand inspection)
            ItemStack offhand = client.player.getOffHandStack();
            boolean unchanged = ItemStack.areEqual(offhand, previousOffhandStack);
            totemOptimizer.updateOffhandState(offhand.isOf(Items.TOTEM_OF_UNDYING), unchanged);
            if (!unchanged) {
                previousOffhandStack = offhand.copy();
            }

            // 3. Crosshair Target & Combat/Interaction Diagnostics
            HitResult hit = client.crosshairTarget;
            if (client.options.attackKey.isPressed()) {
                leftClickOptimizer.recordAttackEvaluation(System.nanoTime() - tickStart);
            }
            if (client.options.useKey.isPressed()) {
                boolean skippedEmptyOffhand = offhand.isEmpty();
                rightClickOptimizer.recordRightClickEvaluation(System.nanoTime() - tickStart, skippedEmptyOffhand);
            }

            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                ItemStack mainHand = client.player.getMainHandStack();
                if (mainHand.getItem() instanceof BlockItem) {
                    blockPlacementOptimizer.checkAndRecordTarget(
                            clientTickCounter, blockHit.getBlockPos(), System.nanoTime() - tickStart
                    );
                }
                if (mainHand.isOf(Items.GLOWSTONE)) {
                    glowstoneOptimizer.recordGlowstoneCheck(System.nanoTime() - tickStart);
                }
                BlockState state = client.world.getBlockState(blockHit.getBlockPos());
                if (state.isOf(Blocks.RESPAWN_ANCHOR)) {
                    int charges = state.get(RespawnAnchorBlock.CHARGES);
                    anchorOptimizer.recordAnchorInspection(charges, System.nanoTime() - tickStart);
                }
            }

            // 4. Crystal Optimizer entity scan profiling
            if (crystalOptimizer.isEnabled()) {
                long scanStart = System.nanoTime();
                Box combatBox = client.player.getBoundingBox().expand(12.0);
                int crystalCount = 0;
                for (Entity entity : client.world.getOtherEntities(client.player, combatBox)) {
                    if (entity instanceof EndCrystalEntity) {
                        crystalCount++;
                    }
                }
                crystalOptimizer.recordCrystalTickScan(crystalCount, System.nanoTime() - scanStart);
            }

            // 5. Periodic Latency & Server Telemetry Sync
            if (clientTickCounter % Math.max(20, latencyOptimizer.getUpdateIntervalTicks()) == 0) {
                if (ClientPlayNetworking.canSend(NetworkProtocol.HandshakeC2SPayload.ID)) {
                    sendServerHandshakeIfPossible();
                } else if (client.getNetworkHandler() != null) {
                    PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                    long ping = entry != null ? entry.getLatency() : 0L;
                    latencyOptimizer.recordSample(ping, 0L, fpsOptimizer.getLatestFrameMicros(), true);
                }
            }
        });

        WorldRenderEvents.END.register(context -> {
            long now = System.nanoTime();
            long frameDuration = now - lastFrameRenderNanos;
            lastFrameRenderNanos = now;
            MinecraftClient client = MinecraftClient.getInstance();
            fpsOptimizer.recordFrame(frameDuration, client.getCurrentFps());
        });
    }

    private void sendServerHandshakeIfPossible() {
        if (ClientPlayNetworking.canSend(NetworkProtocol.HandshakeC2SPayload.ID)) {
            ClientPlayNetworking.send(new NetworkProtocol.HandshakeC2SPayload(
                    NetworkProtocol.PROTOCOL_VERSION,
                    NetworkProtocol.ALL_SERVER_CAPABILITIES,
                    System.nanoTime()
            ));
        }
    }
}
