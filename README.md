# CPVP Optimizer (`1.21.11` Fabric Client + Dedicated Server)

**Developer:** Ahmed  
**Target Minecraft Version:** Java Edition `1.21.11`  
**Mod Loader:** Fabric (`>=0.18.1`)  
**Required Dependencies:** Fabric API (`>=0.140.0+1.21.11`), Java 21  
**Recommended Client Dependency:** Mod Menu (`17.0.0-beta.2`)

---

## Overview

**CPVP Optimizer** is a legitimate, vanilla-compliant Crystal PvP and Respawn Anchor PvP performance optimization and telemetry framework for Minecraft Java Edition `1.21.11`. It features a mandatory split Client + Dedicated Server architecture that eliminates redundant per-tick calculations, bounds heavy explosion/totem particle bursts, profiles input-to-action latency, and provides authoritative server MSPT diagnostics inside a 100% authentic Vanilla Minecraft Options UI.

### Important Fair-Play & Technical Honesty Notice
- **Physical Network Latency:** No Minecraft mod can eliminate physical Internet routing latency (speed-of-light delay between your router and a remote game server). **Latency Optimizer** separates network RTT from Server MSPT and Client Frame Time so you know whether lag is caused by your connection, the server's tick loop, or local frame drops.
- **Vanilla Cooldowns & Server Authority:** All vanilla attack cooldowns, block reach limits, collision checks, inventory rules, and server-side validations remain 100% intact. This mod contains **zero** autoclickers, macros, packet floods, or anti-cheat bypasses.

---

## Implemented Modules (13 Modules Across 5 Categories)

### Combat
1. **Crystal Optimizer:** Caches same-tick `EndCrystalEntity` bounding-box lookups, caps overlapping crystal chain explosion smoke particles, and profiles crystal scan times.
2. **Anchor Optimizer:** Caches targeted Respawn Anchor charge state checks per tick and reduces ambient portal particle overhead.
3. **Totem Optimizer:** Skips redundant HUD offhand dirty-checks when the offhand `ItemStack` is unchanged and scales Totem of Undying pop particle density.
4. **Glowstone Optimizer:** Short-circuits non-anchor block property inspections when holding Glowstone and profiles charge interaction timing.

### Interaction
5. **Block Placement Optimizer:** Caches same-tick block placement raycasts on identical `BlockPos` targets and profiles obsidian/block placement preparation.
6. **Right Click Optimizer:** Skips redundant empty-offhand interaction evaluations and profiles item-use dispatch overhead.
7. **Left Click Optimizer:** Reuses the current tick's crosshair `HitResult` and profiles attack input evaluation without altering vanilla attack cooldowns.
8. **Input Optimizer:** Caches per-tick screen focus state to reduce redundant keybind polling overhead.

### Performance
9. **FPS Optimizer:** Tracks real-time FPS, average frame time, and **1% Low FPS** (99th-percentile frame time) with an optional frame-spike particle guard.
10. **Rendering Optimizer:** Distance-based ambient particle culling and distant non-gameplay decorative label culling.

### Network
11. **Latency Optimizer:** Measures RTT, average RTT, maximum RTT, and spike warnings while separating network ping from server MSPT and client frame time.
12. **Packet Optimizer:** Monitors Client/Server protocol compatibility (`v1`) and bounded diagnostic payload throughput without touching gameplay packets.

### Server
13. **Server Performance Optimizer:** Authoritative server tick duration (MSPT) profiler and bounded per-tick event deduplication for Crystal, Anchor, and Block Placement callbacks.

---

## UI Controls & Navigation

Open via **Mod Menu** (`Mods -> CPVP Optimizer -> Configure`) or press **`O`** in-game:
- **Left-Click on any Module Row:** Toggles the module `ON` / `OFF` immediately and saves configuration.
- **Right-Click on any Module Row:** Opens that module's dedicated **Settings Screen** (sliders, toggles, cycle buttons, live diagnostics, `Reset Defaults`, and `Back`).
- **Scrolling:** Use the mouse wheel, drag the proportional vanilla scrollbar thumb, or click anywhere on the scrollbar track. Scroll position is preserved when returning from a module's settings screen.
- **Safe Mode:** Top-bar `Safe Mode: ON` button immediately disables all CPVP Optimizer hooks.

---

## Installation & Building

### Client Installation
1. Install Fabric Loader `0.18.1+` for Minecraft `1.21.11`.
2. Place `fabric-api-0.141.4+1.21.11.jar`, `modmenu-17.0.0-beta.2.jar`, and `CPVP-Optimizer-Client.jar` into `.minecraft/mods/`.

### Dedicated Server Installation
1. Install Fabric Loader `0.18.1+` on your `1.21.11` server.
2. Place `fabric-api-0.141.4+1.21.11.jar` and `CPVP-Optimizer-Server.jar` into the server's `mods/` directory.

### Building from Source
```bash
./gradlew test build remapJar remapServerJar
```

  - Client Artifact: build/libs/CPVP-Optimizer-Client.jar
  - Server Artifact: build/libs/CPVP-Optimizer-Server.jar
