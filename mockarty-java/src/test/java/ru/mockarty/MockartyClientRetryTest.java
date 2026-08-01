// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.exception.MockartyException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the transient-failure retry layer: the client retries on
 * HTTP 429/502/503/504 (and network errors) up to maxRetries, and does NOT
 * retry when maxRetries is 0 or on a non-retryable 4xx.
 */
class MockartyClientRetryTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private MockartyClient client(int maxRetries) {
        return MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("k")
                .namespace("test-ns")
                .timeout(Duration.ofSeconds(5))
                .maxRetries(maxRetries)
                .build();
    }

    private void serve(AtomicInteger hits, int failTimes, int failStatus, String okBody) {
        server.createContext("/api/v1/health", exchange -> {
            int n = hits.incrementAndGet();
            byte[] body;
            int status;
            if (n <= failTimes) {
                status = failStatus;
                body = ("{\"error\":\"transient\"}").getBytes(StandardCharsets.UTF_8);
            } else {
                status = 200;
                body = okBody.getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    @DisplayName("retries 503 then succeeds within the retry budget")
    void retriesTransientThenSucceeds() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        serve(hits, 2, 503, "{\"status\":\"pass\"}");

        try (MockartyClient c = client(2)) {
            String out = c.get("/api/v1/health", String.class);
            assertTrue(out.contains("pass"));
        }
        assertEquals(3, hits.get(), "two 503s + one success = 3 attempts");
    }

    @Test
    @DisplayName("maxRetries=0 surfaces the first 503 without retrying")
    void noRetryWhenDisabled() {
        AtomicInteger hits = new AtomicInteger();
        serve(hits, 5, 503, "{}");

        try (MockartyClient c = client(0)) {
            assertThrows(MockartyException.class, () -> c.get("/api/v1/health", String.class));
        }
        assertEquals(1, hits.get(), "no retries → exactly one attempt");
    }

    @Test
    @DisplayName("does not retry a non-retryable 400")
    void noRetryOnClientError() {
        AtomicInteger hits = new AtomicInteger();
        serve(hits, 5, 400, "{}");

        try (MockartyClient c = client(3)) {
            assertThrows(MockartyException.class, () -> c.get("/api/v1/health", String.class));
        }
        assertEquals(1, hits.get(), "4xx is terminal → one attempt, no retry");
    }
}
