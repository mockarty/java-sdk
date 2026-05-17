// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.MismatchReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GRPCPluginTest {

    private final GRPCPlugin plugin = new GRPCPlugin();

    @Test
    @DisplayName("Round-trip: identical framed payload matches cleanly")
    void identityRoundTrip() throws IOException {
        byte[] msg = new byte[] {0x08, (byte) 0x96, 0x01};
        byte[] framed = frame(msg);
        assertTrue(plugin.matchRequest("application/grpc", framed, framed.clone()).isEmpty());
    }

    @Test
    @DisplayName("Expected too short → framing.expected mismatch")
    void framingHeaderMissingExpected() {
        List<MismatchReport> ms = plugin.matchRequest("application/grpc",
                new byte[] {1, 2}, frame(new byte[] {0x08, (byte) 0x96}));
        assertEquals(1, ms.size());
        assertEquals("grpc.framing.expected", ms.get(0).matcherType());
    }

    @Test
    @DisplayName("Actual too short → framing.actual mismatch")
    void framingHeaderMissingActual() {
        byte[] expected = frame(new byte[] {0x08, (byte) 0x96});
        List<MismatchReport> ms = plugin.matchRequest("application/grpc",
                expected, new byte[] {0, 0});
        assertEquals(1, ms.size());
        assertEquals("grpc.framing.actual", ms.get(0).matcherType());
    }

    @Test
    @DisplayName("Truncated body (declared length > actual) is flagged")
    void truncatedBody() {
        byte[] expected = frame(new byte[] {0x08, (byte) 0x96, 0x01});
        // Reuse framing header but drop trailing byte
        byte[] truncated = new byte[expected.length - 1];
        System.arraycopy(expected, 0, truncated, 0, truncated.length);
        List<MismatchReport> ms = plugin.matchRequest("application/grpc", expected, truncated);
        assertFalse(ms.isEmpty());
    }

    @Test
    @DisplayName("Different payload bytes inside well-framed messages surface as protobuf mismatch")
    void payloadDivergence() {
        byte[] expected = frame(new byte[] {0x08, 0x01});
        byte[] actual = frame(new byte[] {0x08, (byte) 0x99});
        List<MismatchReport> ms = plugin.matchRequest("application/grpc", expected, actual);
        assertFalse(ms.isEmpty());
        assertTrue(ms.get(0).matcherType().startsWith("protobuf."));
    }

    @Test
    @DisplayName("Both sides empty is treated as trivial pass")
    void bothEmpty() {
        assertTrue(plugin.matchRequest("application/grpc", new byte[0], new byte[0]).isEmpty());
    }

    @Test
    @DisplayName("Content types include grpc-web variants")
    void contentTypes() {
        assertTrue(plugin.supportedContentTypes().contains("application/grpc"));
        assertTrue(plugin.supportedContentTypes().contains("application/grpc-web"));
    }

    // ── helpers ─────────────────────────────────────────────────────────

    /** Wrap a payload in the canonical 5-byte gRPC frame header (uncompressed). */
    private static byte[] frame(byte[] msg) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(0); // not-compressed
            out.write((msg.length >>> 24) & 0xff);
            out.write((msg.length >>> 16) & 0xff);
            out.write((msg.length >>> 8) & 0xff);
            out.write(msg.length & 0xff);
            out.write(msg);
        } catch (IOException ignored) {
            // BAOS doesn't throw
        }
        return out.toByteArray();
    }
}
