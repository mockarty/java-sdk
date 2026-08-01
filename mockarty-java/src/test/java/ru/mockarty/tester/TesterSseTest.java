// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke tests for the Java SSE facet (mirrors the Go/Python SSE port). */
public class TesterSseTest {

    private HttpServer server;
    private String base;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // /stream emits two named `tick` events + one default `message` event,
        // then closes so the client reads to EOF (no listen-window wait).
        server.createContext("/stream", ex -> {
            String body =
                    "event: tick\n" +
                    "data: {\"n\":1}\n" +
                    "id: 1\n" +
                    "\n" +
                    "event: tick\n" +
                    "data: {\"n\":2}\n" +
                    "id: 2\n" +
                    "\n" +
                    ": this is a comment, ignored\n" +
                    "data: {\"hello\":\"world\"}\n" +
                    "\n";
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    @Test
    void sseCollectsEventsAssertsAndExtracts() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.sse("/stream").subscribe()
                .listen(Duration.ofSeconds(2))
                .expectMinEvents(3)
                .expectExactEvents(3)
                .expectEvent("tick")
                .expectEvent("")                       // "" == "message" (the default-type event)
                .expectEventData("message", "{\"hello\":\"world\"}")
                .expectJsonPath("tick", "$.n", 1)      // FIRST tick → n=1
                .extract("message", "$.hello", "h")
                .done();
        t.finish();
        assertTrue(t.ok(), () -> "errors: " + t.errors());
        assertEquals("world", t.vars().get("h"));
    }

    @Test
    void sseMissingEventFails() {
        Tester t = new Tester.Builder().baseUrl(base).build();
        t.sse("/stream").subscribe()
                .listen(Duration.ofSeconds(2))
                .expectEvent("nope")
                .done();
        t.finish();
        assertTrue(!t.ok(), "a missing event must fail the run");
    }
}
