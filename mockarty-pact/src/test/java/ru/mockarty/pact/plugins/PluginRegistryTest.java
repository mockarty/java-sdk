// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.plugins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.MismatchReport;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginRegistryTest {

    @Test
    @DisplayName("global() auto-loads bundled plugins")
    void autoLoad() {
        PluginRegistry r = PluginRegistry.global();
        assertTrue(r.get("protobuf").isPresent(), "protobuf plugin should be auto-loaded");
        assertTrue(r.get("grpc").isPresent(), "grpc plugin should be auto-loaded");
        // Case-insensitive lookup
        assertTrue(r.get("ProtoBuf").isPresent());
    }

    @Test
    @DisplayName("register(): replaces an existing entry under the same name")
    void replaceEntry() {
        PluginRegistry r = PluginRegistry.fresh();
        Plugin p1 = new DummyPlugin("dup", "1");
        Plugin p2 = new DummyPlugin("DUP", "2");
        r.register(p1).register(p2);
        assertEquals(1, r.size());
        assertEquals("2", r.get("dup").orElseThrow().version());
    }

    @Test
    @DisplayName("register() rejects blank/null name")
    void rejectBlank() {
        PluginRegistry r = PluginRegistry.fresh();
        assertThrows(IllegalArgumentException.class,
                () -> r.register(new DummyPlugin("", "1")));
        assertThrows(NullPointerException.class,
                () -> r.register(null));
    }

    @Test
    @DisplayName("Concurrent register / get is race-free")
    void concurrentRegistration() throws Exception {
        final int threads = 32;
        final int perThread = 500;
        PluginRegistry r = PluginRegistry.fresh();
        // Pre-seed a handful so reads + writes interleave on the same keys.
        for (int i = 0; i < 4; i++) r.register(new DummyPlugin("pre-" + i, "v"));

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger fails = new AtomicInteger();
        ConcurrentHashMap<String, Boolean> seen = new ConcurrentHashMap<>();

        Thread[] ts = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            ts[t] = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String name = "t" + tid + "-" + i;
                        r.register(new DummyPlugin(name, "v"));
                        if (r.get(name).isEmpty()) fails.incrementAndGet();
                        seen.put(name, true);
                        // Also read a pre-seeded key
                        if (r.get("pre-0").isEmpty()) fails.incrementAndGet();
                    }
                } catch (Exception ex) {
                    fails.incrementAndGet();
                }
            });
            ts[t].start();
        }
        start.countDown();
        for (Thread thread : ts) thread.join();
        assertEquals(0, fails.get(), "all registrations should round-trip cleanly");
        assertEquals(threads * perThread, seen.size());
    }

    // ── Local fixture ────────────────────────────────────────────────

    private static final class DummyPlugin implements Plugin {
        private final String name;
        private final String version;
        DummyPlugin(String name, String version) { this.name = name; this.version = version; }
        @Override public String name() { return name; }
        @Override public String version() { return version; }
        @Override public Set<String> supportedContentTypes() { return Set.of(); }
        @Override public List<MismatchReport> matchRequest(String ct, byte[] e, byte[] a) {
            return List.of();
        }
    }
}
