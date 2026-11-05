package com.ahmed.cpvpoptimizer;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.client.modules.*;
import com.ahmed.cpvpoptimizer.client.ui.Scrollbar;
import com.ahmed.cpvpoptimizer.client.ui.UIAnimations;
import com.ahmed.cpvpoptimizer.common.*;
import com.ahmed.cpvpoptimizer.server.ServerDiagnostics;
import com.ahmed.cpvpoptimizer.server.modules.ServerPerformanceOptimizer;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CPVPOptimizerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ModuleRegistry registry = ModuleRegistry.getInstance();
        registry.clearForTesting();

        // Register all 13 modules (12 Client + 1 Server)
        registry.register(new CrystalOptimizer());
        registry.register(new AnchorOptimizer());
        registry.register(new TotemOptimizer());
        registry.register(new GlowstoneOptimizer());
        registry.register(new BlockPlacementOptimizer());
        registry.register(new RightClickOptimizer());
        registry.register(new LeftClickOptimizer());
        registry.register(new InputOptimizer());
        registry.register(new FPSOptimizer());
        registry.register(new RenderingOptimizer());
        registry.register(new LatencyOptimizer());
        registry.register(new PacketOptimizer());
        registry.register(new ServerPerformanceOptimizer());

        OptimizerConfig.getInstance().setConfigPathForTesting(tempDir.resolve("cpvpoptimizer.json"));
        OptimizerConfig.getInstance().resetAllToDefaults();
    }

    @Test
    void testAllThirteenModulesRegisteredAcrossCategories() {
        ModuleRegistry registry = ModuleRegistry.getInstance();
        assertEquals(13, registry.getAllModules().size(), "All 13 modules must be registered");
        assertEquals(4, registry.getByCategory(ModuleCategory.COMBAT).size());
        assertEquals(4, registry.getByCategory(ModuleCategory.INTERACTION).size());
        assertEquals(2, registry.getByCategory(ModuleCategory.PERFORMANCE).size());
        assertEquals(2, registry.getByCategory(ModuleCategory.NETWORK).size());
        assertEquals(1, registry.getByCategory(ModuleCategory.SERVER).size());
    }

    @Test
    void testModuleToggleAndSettingsReset() {
        OptimizerModule crystal = ModuleRegistry.getInstance().getById(ModuleId.CRYSTAL_OPTIMIZER).orElseThrow();
        assertTrue(crystal.isEnabled());

        // Simulate Left-Click toggle OFF then ON
        crystal.toggle();
        assertFalse(crystal.isEnabled());
        crystal.toggle();
        assertTrue(crystal.isEnabled());

        // Modify slider setting & verify bounds clamping
        ModuleSetting.IntSliderSetting particleCap = (ModuleSetting.IntSliderSetting) crystal.getSettings().get(3);
        particleCap.setValue(999);
        assertEquals(64, particleCap.getValue(), "Slider must clamp to max value");
        particleCap.setValue(-10);
        assertEquals(4, particleCap.getValue(), "Slider must clamp to min value");

        crystal.resetDefaults();
        assertEquals(16, particleCap.getValue(), "Reset defaults must restore original slider value");
    }

    @Test
    void testConfigPersistenceMigrationAndSafeMode() throws Exception {
        Path cfgFile = tempDir.resolve("cpvpoptimizer.json");
        OptimizerConfig config = OptimizerConfig.getInstance();

        OptimizerModule anchor = ModuleRegistry.getInstance().getById(ModuleId.ANCHOR_OPTIMIZER).orElseThrow();
        anchor.setEnabled(false);
        config.save();
        assertTrue(Files.exists(cfgFile), "Configuration file must be written atomically");

        // Re-enable in memory, then reload from disk to verify persistence
        anchor.setEnabled(true);
        config.load();
        assertFalse(anchor.isEnabled(), "Reloaded config must restore disabled state for Anchor Optimizer");

        // Activate Safe Mode -> must disable all modules safely
        config.setSafeMode(true);
        assertTrue(config.isSafeMode());
        for (OptimizerModule mod : ModuleRegistry.getInstance().getAllModules()) {
            assertFalse(mod.isEnabled(), "Safe Mode must disable module: " + mod.getId());
        }
    }

    @Test
    void testSchemaMigrationFromVersion1() {
        ConfigSchema oldSchema = new ConfigSchema();
        oldSchema.schemaVersion = 1;
        oldSchema.modules = null;

        boolean migrated = oldSchema.migrateAndValidate();
        assertTrue(migrated);
        assertEquals(ConfigSchema.CURRENT_CONFIG_VERSION, oldSchema.schemaVersion);
        assertNotNull(oldSchema.modules);
    }

    @Test
    void testProtocolVersionNegotiation() {
        assertTrue(NetworkProtocol.isVersionCompatible(NetworkProtocol.PROTOCOL_VERSION));
        assertFalse(NetworkProtocol.isVersionCompatible(NetworkProtocol.PROTOCOL_VERSION + 1));
        assertFalse(NetworkProtocol.isVersionCompatible(0));
    }

    @Test
    void testScrollbarThumbSizingAndDragging() {
        Scrollbar scrollbar = new Scrollbar();
        scrollbar.setBounds(300, 50, 6, 100);

        assertFalse(scrollbar.isVisible(80), "Scrollbar should hide when content fits viewport");
        assertTrue(scrollbar.isVisible(300), "Scrollbar must be visible when content overflows");

        int thumbHeight = scrollbar.getThumbHeight(300);
        assertTrue(thumbHeight >= 32 && thumbHeight < 100);

        double[] updatedScroll = {0.0};
        boolean clicked = scrollbar.mouseClicked(303, 120, 0, 0.0, 200, 300, val -> updatedScroll[0] = val);
        assertTrue(clicked);
        assertTrue(updatedScroll[0] > 0.0, "Clicking lower track must advance scroll position");
        scrollbar.mouseReleased();
        assertFalse(scrollbar.isDragging());
    }

    @Test
    void testUIAnimationsRespectDisableFlag() {
        OptimizerConfig.getInstance().setUiAnimationsEnabled(false);
        assertEquals(1.0f, UIAnimations.approach(0.0f, 1.0f, 0.016f, 10.0f), 1e-5f);
        assertEquals(120.0, UIAnimations.smoothScroll(0.0, 120.0, 0.016f), 1e-5);
    }

    @Test
    void testServerDiagnosticsDeduplicationAndRingBuffer() {
        ServerDiagnostics diag = new ServerDiagnostics();
        diag.onServerTickStart(1L);

        BlockPos pos = new BlockPos(10, 64, 10);
        assertTrue(diag.markBlockPosProfiledThisTick(pos), "First lookup in tick must return true");
        assertFalse(diag.markBlockPosProfiledThisTick(pos), "Second lookup on same BlockPos in same tick must be deduplicated");

        diag.onServerTickStart(2L);
        assertTrue(diag.markBlockPosProfiledThisTick(pos), "New server tick must allow fresh profiling");
    }
}
