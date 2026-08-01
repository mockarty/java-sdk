// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-process wire tests for {@link TestRunApi#aggregateRunsReport} — the
 * stateless replacement for the persistent merge surface removed in
 * migration 100. Pins the {@code POST /test-runs/reports/aggregate} contract:
 * {@code run_ids} body, {@code format} query param, raw bytes back.
 */
class TestRunAggregateReportTest {

    private HttpServer server;
    private MockartyClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("test-key")
                .namespace("test-ns")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("aggregateRunsReport POSTs run_ids + format and returns raw bytes")
    void postsRunIdsAndFormat() throws Exception {
        AtomicReference<String> gotQuery = new AtomicReference<>();
        AtomicReference<JsonNode> gotBody = new AtomicReference<>();
        server.createContext("/api/v1/test-runs/reports/aggregate", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            gotQuery.set(exchange.getRequestURI().getQuery());
            gotBody.set(mapper.readTree(exchange.getRequestBody()));
            byte[] body = "# Aggregate test run: nightly\n".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/markdown");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        byte[] out = client.testRuns().aggregateRunsReport(
                "nightly", List.of("r1", "r2"), TestRunApi.AGGREGATE_REPORT_FORMAT_MARKDOWN);

        assertTrue(new String(out, StandardCharsets.UTF_8).contains("Aggregate test run: nightly"));
        assertEquals("format=markdown", gotQuery.get());
        JsonNode body = gotBody.get();
        assertEquals(2, body.get("run_ids").size());
        assertEquals("r1", body.get("run_ids").get(0).asText());
        assertEquals("nightly", body.get("name").asText());
    }

    @Test
    @DisplayName("empty/null format defaults to unified; null name is omitted")
    void defaultsAndOmits() throws Exception {
        AtomicReference<String> gotQuery = new AtomicReference<>();
        AtomicReference<JsonNode> gotBody = new AtomicReference<>();
        server.createContext("/api/v1/test-runs/reports/aggregate", exchange -> {
            gotQuery.set(exchange.getRequestURI().getQuery());
            gotBody.set(mapper.readTree(exchange.getRequestBody()));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        client.testRuns().aggregateRunsReport(null, List.of("r1"), "");

        assertEquals("format=unified", gotQuery.get());
        assertTrue(gotBody.get().get("name") == null, "null name must be omitted from the body");
    }

    @Test
    @DisplayName("empty runIds is rejected client-side")
    void rejectsEmptyRunIds() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testRuns().aggregateRunsReport("x", List.of(), "unified"));
    }
}
