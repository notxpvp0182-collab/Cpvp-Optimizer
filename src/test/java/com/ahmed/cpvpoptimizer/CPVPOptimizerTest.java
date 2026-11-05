package com.ahmed.cpvpoptimizer;

import com.ahmed.cpvpoptimizer.client.config.OptimizerConfig;
import com.ahmed.cpvpoptimizer.client.modules.*;
import com.ahmed.cpvpoptimizer.common.*;
import com.ahmed.cpvpoptimizer.server.modules.ServerPerformanceOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class CPVPOptimizerTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ModuleRegistry registry = ModuleRegistry.getInstance();
        registry.clearForTesting();

        registry.register(new CrystalOptimizer());
        registry.register(new AnchorOptimizer());
        registry.register(new GlowstoneOptimizer());
        registry.register(new TotemOptimizer());
        registry.register(new XPOptimizer());
        registry.register(new BlockPlacementOptimizer());
        registry.register(new HotbarOptimizer());
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
    void testAllFifteenModulesAndMaxResponsivenessProfile() {
        ModuleRegistry registry = ModuleRegistry.getInstance();
        assertEquals(15, registry.getAllModules().size());

        OptimizerConfig.getInstance().applyMaximumResponsivenessProfile();
        BlockPlacementOptimizer place = (BlockPlacementOptimizer) registry.getById(ModuleId.BLOCK_PLACEMENT_OPTIMIZER).orElseThrow();
        assertEquals(0, place.getPlacementCooldownTicks());
        assertTrue(place.isZeroPlaceDelayEnabled());
    }
}
