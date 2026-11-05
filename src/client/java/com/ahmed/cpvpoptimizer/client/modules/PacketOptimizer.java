package com.ahmed.cpvpoptimizer.client.modules;

import com.ahmed.cpvpoptimizer.common.ModuleCategory;
import com.ahmed.cpvpoptimizer.common.ModuleId;
import com.ahmed.cpvpoptimizer.common.ModuleSetting.BooleanSetting;
import com.ahmed.cpvpoptimizer.common.OptimizerModule;
import com.ahmed.cpvpoptimizer.common.RingBufferStats;

import java.util.Locale;

/**
 * MODULE 13: PACKET OPTIMIZER
 * Profiles network payload throughput and protocol compatibility using bounded history.
 * Never forges, drops, duplicates, or reorders gameplay packets.
 */
public final class PacketOptimizer extends OptimizerModule {
    private final BooleanSetting packetDiagnostics;
    private final BooleanSetting protocolVerification;

    private final RingBufferStats inboundBytesPerSec = new RingBufferStats(60);
    private int telemetryPacketsReceived = 0;
    private boolean serverProtocolVerified = false;

    public PacketOptimizer() {
        super(
                ModuleId.PACKET_OPTIMIZER,
                "Packet Optimizer",
                "Bounded network payload diagnostics and client/server protocol capability monitoring.",
                ModuleCategory.NETWORK,
                true
        );

        this.packetDiagnostics = registerSetting(new BooleanSetting(
                "packet_diagnostics", "Network Throughput Profiling",
                "Tracks bounded diagnostic packet statistics.",
                true, null
        ));
        this.protocolVerification = registerSetting(new BooleanSetting(
                "protocol_verification", "Strict Protocol Verification",
                "Verifies CPVP Optimizer server protocol compatibility during handshake.",
                true, null
        ));
    }

    public void recordTelemetryPacket(int payloadBytes, boolean compatible) {
        if (!isEnabled()) return;
        telemetryPacketsReceived++;
        this.serverProtocolVerified = compatible;
        if (packetDiagnostics.getValue()) {
            inboundBytesPerSec.record(payloadBytes);
        }
    }

    public void onDisconnect() {
        this.serverProtocolVerified = false;
    }

    @Override
    protected void onEnable() {}

    @Override
    protected void onDisable() {
        inboundBytesPerSec.clear();
        telemetryPacketsReceived = 0;
        serverProtocolVerified = false;
    }

    @Override
    public String getDiagnosticsSummary() {
        return String.format(
                Locale.ROOT,
                "Server Protocol: %s | Telemetry Syncs: %d | Avg Payload: %.0f B",
                serverProtocolVerified ? "VERIFIED (v1)" : "VANILLA / FALLBACK",
                telemetryPacketsReceived,
                inboundBytesPerSec.average()
        );
    }
}
