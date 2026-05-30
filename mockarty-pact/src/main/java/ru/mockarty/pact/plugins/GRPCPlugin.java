// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import ru.mockarty.pact.MismatchReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Pact V4 gRPC plugin — built-in, NO live gRPC server.
 *
 * <p>The plugin validates the gRPC <i>length-prefixed message</i> framing
 * on a {@code application/grpc} body (5-byte prefix: 1-byte compressed-flag
 * + 4-byte big-endian length, then the message itself) and then delegates
 * payload comparison to {@link ProtobufPlugin}. It does NOT bind a Netty
 * channel, NOT host an {@code ServerCalls} dispatcher, NOT support
 * streaming — those are out of scope for the SDK per
 * {@code feedback_sdk_thin_layer}. Customers running provider verification
 * against a real gRPC service do that via the standard pact-jvm-provider
 * tool; this plugin lives entirely on the contract layer.</p>
 *
 * <p>Why bother at all? Because catching "wrong-protobuf-on-the-wire" at
 * contract test time is the single highest-leverage check for gRPC, and
 * the rest of the gRPC story (codes, trailers, deadlines) is exercised
 * by the provider's own integration tests.</p>
 */
public final class GRPCPlugin implements Plugin {

    static final String NAME = "grpc";
    private static final String VERSION = "0.1.0";
    private static final Set<String> CONTENT_TYPES = Set.of(
            "application/grpc",
            "application/grpc+proto",
            "application/grpc-web",
            "application/grpc-web+proto");

    /** gRPC framing: 1 byte compressed flag + 4 bytes big-endian length. */
    private static final int FRAME_HEADER_LEN = 5;

    private final ProtobufPlugin payload = new ProtobufPlugin();

    @Override public String name() { return NAME; }
    @Override public String version() { return VERSION; }
    @Override public Set<String> supportedContentTypes() { return CONTENT_TYPES; }

    @Override
    public List<MismatchReport> matchRequest(String contentType, byte[] expected, byte[] actual) {
        List<MismatchReport> out = new ArrayList<>();
        byte[] e = expected == null ? new byte[0] : expected;
        byte[] a = actual == null ? new byte[0] : actual;

        // Both sides empty → trivially equal. A real client never sends
        // a zero-byte body on application/grpc but a misconfigured one
        // might — fail loud rather than silently pass.
        if (e.length == 0 && a.length == 0) return out;

        // Frame-length sanity check: anything shorter than the prefix
        // cannot be a valid gRPC message, so report it before delegating.
        if (e.length < FRAME_HEADER_LEN) {
            out.add(new MismatchReport("$.body[grpc.frame]",
                    "expected ≥" + FRAME_HEADER_LEN + " bytes (framing header)",
                    "expected has " + e.length + " bytes",
                    "grpc.framing.expected"));
            return out;
        }
        if (a.length < FRAME_HEADER_LEN) {
            out.add(new MismatchReport("$.body[grpc.frame]",
                    "≥" + FRAME_HEADER_LEN + " bytes (framing header)",
                    a.length + " bytes",
                    "grpc.framing.actual"));
            return out;
        }

        int eLen = readBigEndian32(e, 1);
        int aLen = readBigEndian32(a, 1);
        if (eLen + FRAME_HEADER_LEN != e.length) {
            out.add(new MismatchReport("$.body[grpc.frame.declared-length]",
                    "frame declares " + eLen + " bytes of payload",
                    "expected total " + e.length + " (mismatch in fixture)",
                    "grpc.framing.expected.length"));
        }
        if (aLen + FRAME_HEADER_LEN != a.length) {
            out.add(new MismatchReport("$.body[grpc.frame.declared-length]",
                    "frame declares " + eLen + " bytes of payload",
                    "actual total " + a.length + " (truncated/oversized message)",
                    "grpc.framing.actual.length"));
        }
        if (!out.isEmpty()) return Collections.unmodifiableList(out);

        // Frames look sane → defer payload bytes to the Protobuf plugin.
        byte[] eBody = sliceTail(e, FRAME_HEADER_LEN);
        byte[] aBody = sliceTail(a, FRAME_HEADER_LEN);
        return payload.matchRequest("application/x-protobuf", eBody, aBody);
    }

    @Override
    public byte[] generateResponse(String contentType, byte[] example) {
        // For a response we keep the framing intact: the example is
        // already a fully-framed gRPC message, so just hand it back.
        return example == null ? new byte[0] : example.clone();
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private static int readBigEndian32(byte[] buf, int off) {
        return ((buf[off] & 0xff) << 24)
                | ((buf[off + 1] & 0xff) << 16)
                | ((buf[off + 2] & 0xff) << 8)
                | (buf[off + 3] & 0xff);
    }

    private static byte[] sliceTail(byte[] src, int from) {
        byte[] out = new byte[src.length - from];
        System.arraycopy(src, from, out, 0, out.length);
        return out;
    }
}
