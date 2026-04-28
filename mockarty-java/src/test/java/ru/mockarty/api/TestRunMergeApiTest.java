// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.MergedRunList;
import ru.mockarty.model.MergedRunView;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRunMergeApiTest {

    private HttpServer server;
    private MockartyClient client;

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

    private static String mergedRunPayload(String id, String name) {
        return "{\"ID\":\"" + id + "\",\"Namespace\":\"test-ns\",\"Mode\":\"merged\"," +
                "\"Status\":\"completed\",\"Name\":\"" + name + "\"," +
                "\"StartedAt\":\"2026-04-20T10:00:00Z\",\"UpdatedAt\":\"2026-04-20T10:05:00Z\"," +
                "\"Progress\":100}";
    }

    private static String sourceRunPayload(String id) {
        return "{\"ID\":\"" + id + "\",\"Namespace\":\"test-ns\",\"Mode\":\"functional\"," +
                "\"Status\":\"completed\",\"Name\":\"Smoke\"," +
                "\"StartedAt\":\"2026-04-20T09:55:00Z\",\"UpdatedAt\":\"2026-04-20T09:59:00Z\"," +
                "\"Progress\":100}";
    }

    private void respond(String path, int status, String body) {
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    @Test
    @DisplayName("mergeRuns posts name + source ids and returns populated view")
    void mergeRunsCreates() {
        AtomicReference<String> seenBody = new AtomicReference<>();
        String parentId = "11111111-1111-1111-1111-111111111111";
        String sourceId = "22222222-2222-2222-2222-222222222222";
        server.createContext("/api/v1/test-runs/merges", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                seenBody.set(new String(
                        exchange.getRequestBody().readAllBytes(),
                        StandardCharsets.UTF_8));
                String body = "{\"run\":" + mergedRunPayload(parentId, "Release gate")
                        + ",\"sources\":[" + sourceRunPayload(sourceId) + "]}";
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        });

        MergedRunView view = client.testRuns().mergeRuns("Release gate", List.of(sourceId));
        assertNotNull(view.getRun());
        assertEquals(parentId, view.getRun().getId());
        assertEquals("merged", view.getRun().getMode());
        assertEquals(1, view.getSources().size());
        assertEquals(sourceId, view.getSources().get(0).getId());

        String posted = seenBody.get();
        assertNotNull(posted);
        assertTrue(posted.contains("\"name\":\"Release gate\""));
        assertTrue(posted.contains("\"sourceRunIds\""));
        assertTrue(posted.contains(sourceId));
    }

    @Test
    @DisplayName("mergeRuns rejects empty source list client-side")
    void mergeRunsValidates() {
        assertThrows(IllegalArgumentException.class,
                () -> client.testRuns().mergeRuns("x", List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> client.testRuns().mergeRuns("x", null));
    }

    @Test
    @DisplayName("listMergedRuns decodes the pagination envelope")
    void listMergedRuns() {
        String parentId = "33333333-3333-3333-3333-333333333333";
        respond("/api/v1/test-runs/merges", 200,
                "{\"items\":[{\"run\":" + mergedRunPayload(parentId, "Nightly")
                        + ",\"sources\":[]}],\"total\":1,\"limit\":50,\"offset\":0}");
        MergedRunList page = client.testRuns().listMergedRuns();
        assertEquals(1, page.getTotal());
        assertEquals(50, page.getLimit());
        assertEquals(1, page.getItems().size());
        assertEquals(parentId, page.getItems().get(0).getRun().getId());
    }

    @Test
    @DisplayName("getMergedRun returns parent + sources")
    void getMergedRun() {
        String parentId = "44444444-4444-4444-4444-444444444444";
        String sourceId = "55555555-5555-5555-5555-555555555555";
        respond("/api/v1/test-runs/merges/" + parentId, 200,
                "{\"run\":" + mergedRunPayload(parentId, "Gate")
                        + ",\"sources\":[" + sourceRunPayload(sourceId) + "]}");
        MergedRunView view = client.testRuns().getMergedRun(parentId);
        assertNotNull(view.getRun());
        assertEquals("completed", view.getRun().getStatus());
        assertEquals(sourceId, view.getSources().get(0).getId());
    }

    @Test
    @DisplayName("deleteMergedRun issues a DELETE on the parent path")
    void deleteMergedRun() {
        AtomicInteger calls = new AtomicInteger();
        String parentId = "66666666-6666-6666-6666-666666666666";
        server.createContext("/api/v1/test-runs/merges/" + parentId, exchange -> {
            if ("DELETE".equals(exchange.getRequestMethod())) {
                calls.incrementAndGet();
                exchange.sendResponseHeaders(204, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.close();
        });
        client.testRuns().deleteMergedRun(parentId);
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("getMergedRunReport defaults to the unified format")
    void getMergedRunReportDefault() {
        AtomicReference<String> seenQuery = new AtomicReference<>();
        String parentId = "77777777-7777-7777-7777-777777777777";
        server.createContext("/api/v1/test-runs/merges/" + parentId + "/report",
                exchange -> {
                    seenQuery.set(exchange.getRequestURI().getQuery());
                    byte[] bytes = "{\"mergedRunId\":\"x\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
        byte[] body = client.testRuns().getMergedRunReport(parentId, null);
        assertNotNull(body);
        assertTrue(body.length > 0);
        assertEquals("format=unified", seenQuery.get());
    }

    @Test
    @DisplayName("getMergedRunReport passes markdown when requested")
    void getMergedRunReportMarkdown() {
        AtomicReference<String> seenQuery = new AtomicReference<>();
        String parentId = "88888888-8888-8888-8888-888888888888";
        server.createContext("/api/v1/test-runs/merges/" + parentId + "/report",
                exchange -> {
                    seenQuery.set(exchange.getRequestURI().getQuery());
                    byte[] bytes = "# merged run\n".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/markdown");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                });
        byte[] body = client.testRuns().getMergedRunReport(
                parentId, TestRunApi.MERGED_RUN_REPORT_FORMAT_MARKDOWN);
        assertNotNull(body);
        assertTrue(new String(body, StandardCharsets.UTF_8).startsWith("# merged run"));
        assertEquals("format=markdown", seenQuery.get());
    }

    @Test
    @DisplayName("getMergedRunReport surfaces non-2xx as MockartyException")
    void getMergedRunReportError() {
        String parentId = "99999999-9999-9999-9999-999999999999";
        respond("/api/v1/test-runs/merges/" + parentId + "/report", 404,
                "{\"error\":\"not found\"}");
        assertThrows(MockartyException.class,
                () -> client.testRuns().getMergedRunReport(parentId, null));
    }
}
