# Client/Server Networking Protocol (`v1`)

- **Payloads:**
  - `cpvpoptimizer:handshake_c2s` (`HandshakeC2SPayload`): Sends client protocol version (`1`), requested capability bitmask, and monotonic timestamp (`System.nanoTime()`).
  - `cpvpoptimizer:telemetry_s2c` (`TelemetryS2CPayload`): Responds with server protocol version (`1`), active server capabilities, echoed client timestamp for accurate RTT calculation, average/max server tick duration in microseconds, and windowed Crystal/Anchor/Placement event counts.
- **Safety & Fallback:**
  - Rate-limited to at most 1 sync per 1,000 ms per player (`MIN_SYNC_INTERVAL_MS`).
  - If the remote server does not have `CPVP-Optimizer-Server.jar` installed, `ClientPlayNetworking.canSend(...)` returns `false`, and the client UI explicitly displays `Server Mod: Not Installed (Client-Only Mode)`.
