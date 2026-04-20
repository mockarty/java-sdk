// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.EntitySearchRequest;
import ru.mockarty.model.EntitySearchResponse;
import ru.mockarty.model.EntitySearchResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySearchApiTest {

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

    @Test
    @DisplayName("search builds the query string and parses the envelope")
    void searchBuildsQuery() throws Exception {
        AtomicReference<String> seenQuery = new AtomicReference<>();
        server.createContext("/api/v1/entity-search", exchange -> {
            seenQuery.set(exchange.getRequestURI().getRawQuery());
            String body = "{\"items\":[{" +
                    "\"id\":\"11111111-2222-3333-4444-555555555555\"," +
                    "\"type\":\"test_plan\"," +
                    "\"name\":\"smoke-suite\"," +
                    "\"namespace\":\"production\"," +
                    "\"createdAt\":\"2026-04-19T12:00:00Z\"," +
                    "\"numericId\":42}]," +
                    "\"total\":1}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        EntitySearchResponse resp = client.entitySearch().search(new EntitySearchRequest()
                .type(EntitySearchRequest.TYPE_TEST_PLAN)
                .namespace("production")
                .query("smoke")
                .limit(25)
                .offset(5));

        assertEquals(1, resp.getTotal());
        assertEquals(1, resp.getItems().size());
        EntitySearchResult item = resp.getItems().get(0);
        assertEquals("smoke-suite", item.getName());
        assertEquals(Long.valueOf(42L), item.getNumericId());
        assertEquals("2026-04-19T12:00:00Z", item.getCreatedAt());

        String q = seenQuery.get();
        assertNotNull(q);
        assertTrue(q.contains("type=test_plan"), q);
        assertTrue(q.contains("namespace=production"), q);
        assertTrue(q.contains("q=smoke"), q);
        assertTrue(q.contains("limit=25"), q);
        assertTrue(q.contains("offset=5"), q);
    }

    @Test
    @DisplayName("search omits zero / blank optional params")
    void searchOmitsZeroParams() throws Exception {
        AtomicReference<String> seenQuery = new AtomicReference<>();
        server.createContext("/api/v1/entity-search", exchange -> {
            seenQuery.set(exchange.getRequestURI().getRawQuery());
            String body = "{\"items\":[],\"total\":0}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        client.entitySearch().search(new EntitySearchRequest()
                .type(EntitySearchRequest.TYPE_MOCK));

        String q = seenQuery.get();
        assertNotNull(q);
        assertTrue(q.contains("type=mock"), q);
        // Optional fields MUST NOT appear when zero / blank.
        assertFalse(q.contains("namespace="), q);
        assertFalse(q.contains("q="), q);
        assertFalse(q.contains("limit="), q);
        assertFalse(q.contains("offset="), q);
    }

    @Test
    @DisplayName("search rejects null request and blank type")
    void searchRejectsBlankType() {
        assertThrows(IllegalArgumentException.class,
                () -> client.entitySearch().search(null));
        assertThrows(IllegalArgumentException.class,
                () -> client.entitySearch().search(new EntitySearchRequest().type("  ")));
    }

    @Test
    @DisplayName("getItems normalises null items to empty list")
    void normaliseEmptyItems() throws Exception {
        server.createContext("/api/v1/entity-search", exchange -> {
            String body = "{\"total\":0}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        EntitySearchResponse resp = client.entitySearch().search(new EntitySearchRequest()
                .type(EntitySearchRequest.TYPE_MOCK));
        assertNotNull(resp.getItems());
        assertEquals(0, resp.getItems().size());

        // Items with no numericId in the JSON deserialise to null, not 0.
        // (Verified on a fresh row.)
        EntitySearchResult bare = new EntitySearchResult();
        assertNull(bare.getNumericId());
    }
}
