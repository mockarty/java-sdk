// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.ExperienceRecordRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperienceApiTest {
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("k").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void searchAndRecord() throws Exception {
        server.createContext("/api/v1/autotester/context/knowledge/search", exchange -> reply(exchange, "{\"results\":[{\"id\":\"e1\",\"kind\":\"pitfall\",\"text\":\"retry\",\"source\":\"run\",\"provenance\":\"external\"}],\"total\":1,\"available\":true}"));
        server.createContext("/api/v1/autotester/context/knowledge", exchange -> reply(exchange, "{\"id\":\"e1\",\"kind\":\"pitfall\",\"provenance\":\"external\"}"));
        assertEquals("e1", client.experience().search("retry", List.of("pitfall"), null, 5).getResults().get(0).getId());
        assertEquals("external", client.experience().record(new ExperienceRecordRequest().text("retry").source("run").kind("pitfall")).getProvenance());
    }

    @Test void validatesRequiredFields() {
        assertThrows(IllegalArgumentException.class, () -> client.experience().search("  "));
        assertThrows(IllegalArgumentException.class, () -> client.experience().record(new ExperienceRecordRequest().text("x")));
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
