// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parity coverage for the Java Tester ergonomics that Go/Python already shipped:
 * {@code eventually()} (retry-until with roll-back) and {@code parallel()}
 * (concurrent branches merged deterministically).
 */
class TesterEventuallyParallelTest {

    private HttpServer server;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"ok\":true}".getBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void eventuallyRetriesUntilSuccess() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        AtomicInteger n = new AtomicInteger();
        boolean ok = t.eventually(Duration.ofSeconds(2), Duration.ofMillis(10),
                () -> n.incrementAndGet() >= 3);
        assertTrue(ok, "should succeed once the attempt passes");
        assertEquals(3, n.get(), "should stop on the first passing attempt");
        t.close();
    }

    @Test
    void eventuallyTimesOutReturnsFalse() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        boolean ok = t.eventually(Duration.ofMillis(80), Duration.ofMillis(10), () -> false);
        assertFalse(ok, "never-passing attempt must time out to false");
        t.close();
    }

    @Test
    void parallelRunsBranchesAndMergesDeterministically() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.parallel(
                b -> b.http().get("/ok").expectStatus(200),
                b -> b.http().get("/ok").expectStatus(200),
                b -> b.http().get("/ok").expectStatus(200));
        t.finish();
        assertTrue(t.ok(), "all branches should pass: " + t.errors());
        assertEquals(3, t.report().size(), "each branch contributes one step");
        t.close();
    }

    @Test
    void parallelIsolatesBranchVariables() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.setVar("shared", "parent");
        t.parallel(
                b -> b.setVar("shared", "branch-a"),
                b -> b.setVar("shared", "branch-b"));
        // Branch writes do not leak back to the parent.
        assertEquals("parent", t.vars().get("shared"));
        t.close();
    }
}
