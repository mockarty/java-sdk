package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EffectReconciliationApiTest {
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/admin/effects/reconciliation", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/reconcile")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                if (!body.contains("\"executionId\":\"effect-1\"") || !body.contains("\"autoClaim\":true")) {
                    throw new AssertionError("unexpected reconcile body: " + body);
                }
                reply(exchange, "{\"executionId\":\"effect-1\",\"status\":\"no_effect\"}");
            } else {
                String query = exchange.getRequestURI().getRawQuery();
                if (!query.contains("namespace=team%20a") || !query.contains("family=llm.chat") ||
                        !query.contains("minAgeSeconds=60") || !query.contains("limit=25")) {
                    throw new AssertionError("unexpected queue query: " + query);
                }
                reply(exchange, "{\"items\":[],\"nextCursor\":\"next\"}");
            }
        });
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("k").namespace("team a").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void listAndReconcileNoEffect() throws Exception {
        assertEquals("next", client.effectReconciliation().listQueue(null, "llm.chat", null, 60, 25, null).get("nextCursor"));
        assertEquals("no_effect", client.effectReconciliation().reconcileNoEffect("effect-1", "invoice-1", "provider_invoice").get("status"));
        assertThrows(IllegalArgumentException.class,
                () -> client.effectReconciliation().reconcileNoEffect(" ", "", ""));
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
