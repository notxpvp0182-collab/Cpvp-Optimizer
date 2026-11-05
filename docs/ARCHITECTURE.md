# Architecture Overview

CPVP Optimizer uses Fabric Loom's `splitEnvironmentSourceSets()` architecture:

1. **`src/main/java` (Common + Dedicated Server):**
   - Contains `ModuleRegistry`, `OptimizerModule`, `ModuleSetting`, `ConfigSchema`, `RingBufferStats`, `NetworkProtocol`, and `ServerPerformanceManager`.
   - Zero references to `net.minecraft.client.*`, ensuring `CPVP-Optimizer-Server.jar` loads cleanly on headless Dedicated Servers.
2. **`src/client/java` (Client + UI + Mod Menu):**
   - Contains `CPVPOptimizerClient`, `CPVPModMenuIntegration`, `OptimizerConfig`, Vanilla UI screens (`OptimizerScreen`, `SettingsScreen`, `ScrollableModuleList`, `Scrollbar`, `ModuleButton`, `UIAnimations`), and the 12 client optimizer modules.
