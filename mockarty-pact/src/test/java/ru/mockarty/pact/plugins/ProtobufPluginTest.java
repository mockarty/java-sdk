// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.MismatchReport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtobufPluginTest {

    private final ProtobufPlugin plugin = new ProtobufPlugin();

    @Test
    @DisplayName("Identity round-trip: equal bytes → no mismatch")
    void identityMatch() {
        byte[] payload = new byte[] {0x08, (byte) 0x96, 0x01, 0x12, 0x05, 'h', 'e', 'l', 'l', 'o'};
        assertTrue(plugin.matchRequest("application/x-protobuf", payload, payload.clone()).isEmpty());
    }

    @Test
    @DisplayName("Length mismatch surfaces with protobuf.length tag")
    void lengthMismatch() {
        byte[] expected = new byte[] {0x08, (byte) 0x96};
        byte[] actual = new byte[] {0x08};
        List<MismatchReport> ms = plugin.matchRequest("application/x-protobuf", expected, actual);
        assertEquals(1, ms.size());
        assertEquals("protobuf.length", ms.get(0).matcherType());
    }

    @Test
    @DisplayName("Byte-level mismatch reports first differing byte only")
    void byteMismatch() {
        byte[] expected = new byte[] {0x08, 0x01, 0x02, 0x03};
        byte[] actual = new byte[] {0x08, 0x01, (byte) 0x99, 0x03};
        List<MismatchReport> ms = plugin.matchRequest("application/x-protobuf", expected, actual);
        assertEquals(1, ms.size(), "first divergence should short-circuit");
        assertEquals("protobuf.byte", ms.get(0).matcherType());
        assertTrue(ms.get(0).path().contains("[2]"));
    }

    @Test
    @DisplayName("Null payload tolerated as empty (no NPE)")
    void nullSafety() {
        assertTrue(plugin.matchRequest("application/x-protobuf", null, null).isEmpty());
        assertTrue(plugin.matchRequest("application/x-protobuf", new byte[0], null).isEmpty());
    }

    @Test
    @DisplayName("Content type set covers the canonical three names")
    void contentTypes() {
        assertTrue(plugin.supportedContentTypes().contains("application/x-protobuf"));
        assertTrue(plugin.supportedContentTypes().contains("application/protobuf"));
        assertTrue(plugin.supportedContentTypes().contains("application/vnd.google.protobuf"));
    }

    @Test
    @DisplayName("generateResponse returns a defensive copy")
    void generateDefensiveCopy() {
        byte[] example = new byte[] {1, 2, 3};
        byte[] generated = plugin.generateResponse("application/x-protobuf", example);
        assertNotSame(example, generated);
        assertEquals(3, generated.length);
        generated[0] = 99;
        assertEquals(1, example[0], "mutation must not leak to caller");
    }
}
