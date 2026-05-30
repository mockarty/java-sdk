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
import ru.mockarty.model.ExternalRunRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-process wire tests for {@link ExternalRunsApi#reportBatch}. Spins
 * up a {@link HttpServer} on a free port, has the SDK POST against it,
 * asserts URL + body shape match the canonical
 * {@code /tcm/external-runs/batch} contract.
 */
class ExternalRunsBatchTest {

    private HttpServer server;
    private MockartyClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("k")
                .namespace("qa")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("reportBatch POSTs {runs:[…]} to /tcm/external-runs/batch")
    void postsRunsEnvelope() throws Exception {
        // Pin the path + body shape the SDK is expected to emit.
        server.createContext("/api/v1/namespaces/qa/tcm/external-runs/batch", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            JsonNode req = mapper.readTree(exchange.getRequestBody());
            assertTrue(req.has("runs"), "POST body must carry runs[]");
            assertEquals(2, req.get("runs").size(), "all 2 runs must round-trip");
            assertEquals("a", req.get("runs").get(0).get("caseName").asText());

            byte[] body = ("{\"results\":[" +
                    "{\"index\":0,\"result\":{\"runId\":\"r1\"}}," +
                    "{\"index\":1,\"result\":{\"runId\":\"r2\"}}" +
                    "],\"counts\":{\"total\":2,\"passed\":2,\"failed\":0}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        List<ExternalRunRequest> reqs = Arrays.asList(
                new ExternalRunRequest()
                        .status(ExternalRunRequest.STATUS_PASSED)
                        .caseName("a")
                        .framework("junit5"),
                new ExternalRunRequest()
                        .status(ExternalRunRequest.STATUS_PASSED)
                        .caseName("b")
                        .framework("junit5"));

        JsonNode out = client.externalRuns().reportBatch("qa", reqs);
        assertNotNull(out);
        assertEquals(2, out.get("counts").get("total").asInt());
        assertEquals("r1", out.get("results").get(0).get("result").get("runId").asText());
    }

    @Test
    @DisplayName("reportBatch rejects empty list locally (no network round-trip)")
    void rejectsEmpty() {
        assertThrows(IllegalArgumentException.class, () ->
                client.externalRuns().reportBatch("qa", Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () ->
                client.externalRuns().reportBatch("qa", null));
    }

    @Test
    @DisplayName("reportBatch requires non-empty namespace")
    void rejectsEmptyNamespace() {
        assertThrows(IllegalArgumentException.class, () ->
                client.externalRuns().reportBatch("",
                        Collections.singletonList(new ExternalRunRequest().caseName("a"))));
        assertThrows(IllegalArgumentException.class, () ->
                client.externalRuns().reportBatch(null,
                        Collections.singletonList(new ExternalRunRequest().caseName("a"))));
    }

    @Test
    @DisplayName("reportBatch surfaces server error (5xx)")
    void serverError() {
        server.createContext("/api/v1/namespaces/qa/tcm/external-runs/batch", exchange -> {
            byte[] body = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        assertThrows(MockartyException.class, () ->
                client.externalRuns().reportBatch("qa",
                        Collections.singletonList(new ExternalRunRequest().caseName("a"))));
    }
}
