package com.ahmed.cpvpoptimizer.common;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Versioned and rate-limited Client <-> Server diagnostic & capability handshake protocol.
 * Never overrides or replaces vanilla gameplay packets.
 */
public final class NetworkProtocol {
    public static final String MOD_ID = "cpvpoptimizer";
    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_PAYLOAD_BYTES = 256;
    public static final long MIN_SYNC_INTERVAL_MS = 1000L;

    // Capability bitmask flags
    public static final int CAP_CRYSTAL_DIAGNOSTICS = 1;
    public static final int CAP_ANCHOR_DIAGNOSTICS = 1 << 1;
    public static final int CAP_BLOCK_PLACEMENT_DIAGNOSTICS = 1 << 2;
    public static final int CAP_SERVER_TICK_TELEMETRY = 1 << 3;
    public static final int ALL_SERVER_CAPABILITIES =
            CAP_CRYSTAL_DIAGNOSTICS | CAP_ANCHOR_DIAGNOSTICS | CAP_BLOCK_PLACEMENT_DIAGNOSTICS | CAP_SERVER_TICK_TELEMETRY;

    private NetworkProtocol() {}

    public static boolean isVersionCompatible(int remoteVersion) {
        return remoteVersion == PROTOCOL_VERSION;
    }

    /**
     * Client -> Server Hello & Telemetry Request Payload
     */
    public record HandshakeC2SPayload(int protocolVersion, int requestedCapabilities, long clientTimestampNanos) implements CustomPayload {
        public static final CustomPayload.Id<HandshakeC2SPayload> ID =
                new CustomPayload.Id<>(Identifier.of(MOD_ID, "handshake_c2s"));

        public static final PacketCodec<RegistryByteBuf, HandshakeC2SPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, HandshakeC2SPayload::protocolVersion,
                PacketCodecs.VAR_INT, HandshakeC2SPayload::requestedCapabilities,
                PacketCodecs.VAR_LONG, HandshakeC2SPayload::clientTimestampNanos,
                HandshakeC2SPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Server -> Client Handshake Ack & Performance Telemetry Snapshot
     */
    public record TelemetryS2CPayload(
            int protocolVersion,
            int activeServerCapabilities,
            long echoedClientTimestampNanos,
            int avgTickMicros,
            int maxTickMicros,
            int crystalEventsLastWindow,
            int anchorEventsLastWindow,
            int placementEventsLastWindow
    ) implements CustomPayload {
        public static final CustomPayload.Id<TelemetryS2CPayload> ID =
                new CustomPayload.Id<>(Identifier.of(MOD_ID, "telemetry_s2c"));

        public static final PacketCodec<RegistryByteBuf, TelemetryS2CPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, TelemetryS2CPayload::protocolVersion,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::activeServerCapabilities,
                PacketCodecs.VAR_LONG, TelemetryS2CPayload::echoedClientTimestampNanos,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::avgTickMicros,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::maxTickMicros,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::crystalEventsLastWindow,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::anchorEventsLastWindow,
                PacketCodecs.VAR_INT, TelemetryS2CPayload::placementEventsLastWindow,
                TelemetryS2CPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
