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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public final class CPVPOptimizerClient implements ClientModInitializer {
    private static final KeyBinding.Category KEY_CATEGORY =
            KeyBinding.Category.create(Identifier.of(NetworkProtocol.MOD_ID, "general"));

    private static CPVPOptimizerClient instance;

    private LatencyOptimizer latencyOptimizer;
    private InputOptimizer inputOptimizer;
    private HotbarOptimizer hotbarOptimizer;
    private LeftClickOptimizer leftClickOptimizer;
    private RightClickOptimizer rightClickOptimizer;
    private BlockPlacementOptimizer blockPlacementOptimizer;
    private CrystalOptimizer crystalOptimizer;
    private AnchorOptimizer anchorOptimizer;
    private TotemOptimizer totemOptimizer;
    private GlowstoneOptimizer glowstoneOptimizer;
    private XPOptimizer xpOptimizer;
    private FPSOptimizer fpsOptimizer;
    private RenderingOptimizer renderingOptimizer;
    private PacketOptimizer packetOptimizer;

    private KeyBinding openGuiKeyBinding;
    private long clientTickCounter = 0L;
    private long lastFrameRenderNanos = System.nanoTime();
    private Item previousOffhandItem = Items.AIR;
    private int previousOffhandCount = 0;

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
                KEY_CATEGORY
        ));

        registerNetworkHandlers();
        registerTickAndRenderHooks();

        CPVPOptimizerCommon.LOGGER.info("[CPVP Optimizer] High-responsiveness client initialized.");
    }

    public static CPVPOptimizerClient getInstance() {
        return instance;
    }

    public HotbarOptimizer getHotbarOptimizer() { return hotbarOptimizer; }
    public LeftClickOptimizer getLeftClickOptimizer() { return leftClickOptimizer; }
    public RightClickOptimizer getRightClickOptimizer() { return rightClickOptimizer; }
    public BlockPlacementOptimizer getBlockPlacementOptimizer() { return blockPlacementOptimizer; }
    public CrystalOptimizer getCrystalOptimizer() { return crystalOptimizer; }
    public AnchorOptimizer getAnchorOptimizer() { return anchorOptimizer; }
    public GlowstoneOptimizer getGlowstoneOptimizer() { return glowstoneOptimizer; }
    public XPOptimizer getXpOptimizer() { return xpOptimizer; }

    public void registerClientModules() {
        ModuleRegistry registry = ModuleRegistry.getInstance();

        // Combat Category
        this.crystalOptimizer = new CrystalOptimizer();
        this.anchorOptimizer = new AnchorOptimizer();
        this.glowstoneOptimizer = new GlowstoneOptimizer();
        this.totemOptimizer = new TotemOptimizer();
        this.xpOptimizer = new XPOptimizer();
        registry.register(crystalOptimizer);
        registry.register(anchorOptimizer);
        registry.register(glowstoneOptimizer);
        registry.register(totemOptimizer);
        registry.register(xpOptimizer);

        // Interaction Category
        this.blockPlacementOptimizer = new BlockPlacementOptimizer();
        this.hotbarOptimizer = new HotbarOptimizer();
        this.rightClickOptimizer = new RightClickOptimizer();
        this.leftClickOptimizer = new LeftClickOptimizer();
        this.inputOptimizer = new InputOptimizer();
        registry.register(blockPlacementOptimizer);
        registry.register(hotbarOptimizer);
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
            inputOptimizer.onClientInputTick(client.currentScreen != null, System.nanoTime() - tickStart);

            // Zero-allocation offhand Totem state check (no ItemStack.copy() garbage)
            ItemStack offhand = client.player.getOffHandStack();
            Item currentOffhandItem = offhand.getItem();
            int currentOffhandCount = offhand.getCount();
            boolean unchanged = (currentOffhandItem == previousOffhandItem && currentOffhandCount == previousOffhandCount);
            totemOptimizer.updateOffhandState(offhand.isOf(Items.TOTEM_OF_UNDYING), unchanged);
            previousOffhandItem = currentOffhandItem;
            previousOffhandCount = currentOffhandCount;

            HitResult hit = client.crosshairTarget;
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                blockPlacementOptimizer.checkAndRecordTarget(clientTickCounter, blockHit.getBlockPos());
            }

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

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
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
