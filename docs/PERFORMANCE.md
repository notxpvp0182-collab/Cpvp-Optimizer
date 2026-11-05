# Performance Engineering Design

- **Zero-Allocation Telemetry:** All high-frequency metrics (frame times, server tick durations, RTT history, input timings) are stored in pre-allocated primitive `long[]` ring buffers (`RingBufferStats`) with zero per-frame heap allocations.
- **Atomic Configuration I/O:** `OptimizerConfig` never writes to disk during frame rendering. It writes to `.tmp` and performs an `ATOMIC_MOVE` only when a user clicks a control or closes the screen.
- **Bounded Server Caches:** `ServerDiagnostics` caps per-tick `BlockPos` deduplication maps at `256` entries and clears them on tick boundaries to guarantee zero memory leaks over multi-day server uptimes.
