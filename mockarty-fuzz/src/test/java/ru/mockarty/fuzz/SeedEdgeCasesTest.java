// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedEdgeCasesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void unicodePayloadRoundTrips() throws Exception {
        String payload = "{\"text\":\"日本語 🦊 \\u0000\\u001F\"}";
        Target t = Target.named("u")
                .httpEndpoint("POST", "/x")
                .seeds(Seed.of("unicode", payload))
                .mutator(Mutator.JSON)
                .build();
        JsonNode node = MAPPER.readTree(t.toJson());
        // Jackson normalises the JSON escapes — the parsed body string
        // must match what we put in (modulo legal JSON-equivalent escapes).
        assertEquals(payload, node.get("seedRequests").get(0).get("body").asText(),
                "unicode payload survived round-trip via JSON");
    }

    @Test
    void emptyStringPayloadIsLegal() throws Exception {
        Target t = Target.named("empty")
                .httpEndpoint("GET", "/x")
                .seeds(Seed.of("zero-len", ""))
                .mutator(Mutator.STRING)
                .build();
        JsonNode body = MAPPER.readTree(t.toJson())
                .get("seedRequests").get(0).get("body");
        assertEquals("", body.asText());
    }

    @Test
    void textAccessOnBinarySeedThrows() {
        Seed s = Seed.bytes("b", new byte[]{1, 2, 3});
        assertThrows(IllegalStateException.class, s::text);
    }

    @Test
    void bytesAccessOnTextSeedReturnsUtf8Encoding() {
        Seed s = Seed.of("t", "hello");
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), s.bytes());
    }

    @Test
    void seedBytesAreDefensivelyCopied() {
        byte[] original = {1, 2, 3};
        Seed s = Seed.bytes("b", original);
        original[0] = 99;                // mutate caller's array
        assertEquals(1, s.bytes()[0]);    // seed unaffected
        byte[] returned = s.bytes();
        returned[0] = 88;                 // mutate returned copy
        assertEquals(1, s.bytes()[0]);    // still unaffected
    }

    @Test
    void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> Seed.of("", "x"));
        assertThrows(IllegalArgumentException.class, () -> Seed.of(" ", "x"));
        assertThrows(IllegalArgumentException.class, () -> Seed.of(null, "x"));
    }

    @Test
    void nullPayloadRejected() {
        assertThrows(NullPointerException.class, () -> Seed.of("a", null));
        assertThrows(NullPointerException.class, () -> Seed.bytes("a", null));
    }

    @Test
    void fromFileLoadsContents(@TempDir Path tmp) throws IOException {
        Path p = tmp.resolve("seed.bin");
        Files.write(p, new byte[]{(byte) 0xAA, 0x55, 0x00, (byte) 0xFF});
        Seed s = Seed.fromFile("disk", p);
        assertArrayEquals(new byte[]{(byte) 0xAA, 0x55, 0x00, (byte) 0xFF}, s.bytes());
        assertEquals("disk", s.name());
    }

    @Test
    void parallelTargetBuildingIsSafe() throws Exception {
        // Each builder must be confined to its own thread (mutable!), but
        // the *immutable* Targets they produce should be safe to share
        // across threads. We hammer that contract here.
        int workers = 16;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < workers; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> Target.named("t-" + idx)
                        .httpEndpoint("POST", "/x")
                        .seeds(Seed.of("s", String.valueOf(idx)))
                        .mutator(Mutator.JSON)
                        .build()
                        .toJson()));
            }
            for (int i = 0; i < workers; i++) {
                String json = futures.get(i).get(10, TimeUnit.SECONDS);
                assertTrue(json.contains("\"name\":\"t-" + i + "\""));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void seedEqualityRespectsBinaryContents() {
        Seed a = Seed.bytes("x", new byte[]{1, 2, 3});
        Seed b = Seed.bytes("x", new byte[]{1, 2, 3});
        Seed c = Seed.bytes("x", new byte[]{1, 2, 4});
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
