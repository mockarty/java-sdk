// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-process wire test for {@link MockApi#logs}. The server returns a
 * {@code model.LogsMock} envelope ({@code {"id","requests":[...]}}), NOT a
 * bare array — deserializing as a List threw on every call, so this pins the
 * real contract and that the rows are pulled out of {@code requests}.
 */
class MockApiLogsTest {

    private HttpServer server;
    private MockartyClient client;

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

    private void respond(String json) {
        server.createContext("/api/v1/mocks/m1/logs", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
    }

    @Test
    @DisplayName("logs() reads rows from the LogsMock 'requests' envelope")
    void readsRequestsEnvelope() throws Exception {
        respond("{\"id\":\"m1\",\"requests\":["
                + "{\"id\":\"l1\",\"method\":\"GET\"},"
                + "{\"id\":\"l2\",\"method\":\"POST\"}]}");

        List<Map<String, Object>> logs = client.mocks().logs("m1");

        assertEquals(2, logs.size(), "both request rows must surface");
        assertEquals("l1", logs.get(0).get("id"));
        assertEquals("POST", logs.get(1).get("method"));
    }

    @Test
    @DisplayName("logs() returns an empty list when there are no requests")
    void emptyRequestsYieldsEmptyList() throws Exception {
        respond("{\"id\":\"m1\",\"requests\":[]}");

        List<Map<String, Object>> logs = client.mocks().logs("m1");

        assertTrue(logs.isEmpty(), "no rows -> empty list, never a throw");
    }
}
