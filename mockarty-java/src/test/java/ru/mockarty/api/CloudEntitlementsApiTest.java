// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudEntitlementsApiTest {
    @Test void getUsesExplicitSpaceQuery() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/entitlements", exchange -> {
            assertEquals("space_id=space-1", exchange.getRequestURI().getQuery());
            assertEquals("Bearer cloud-token", exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"revision\":7,\"snapshot\":{\"plan\":\"team\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("cloud-token").timeout(Duration.ofSeconds(5)).maxRetries(0).build()) {
            Map<String, Object> projection = client.cloudEntitlements().get("space-1");
            assertEquals(7, ((Number) projection.get("revision")).intValue());
            assertThrows(IllegalArgumentException.class, () -> client.cloudEntitlements().get(" "));
        } finally {
            server.stop(0);
        }
    }
}
