// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSessionRecoveryApiTest {
    private static final String SESSION_ID = "00000000-0000-4000-8000-000000000601";

    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("test-api-key")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void recoveryMethodsPreserveQueriesAndAcknowledgement() {
        server.createContext("/api/v1/agent/sessions/legacy", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            if (path.endsWith("/export")) {
                assertTrue(query.contains("limit=10"));
                assertTrue(query.contains("afterEventId=7"));
                respond(exchange, "{\"session\":{\"id\":\"" + SESSION_ID + "\"},\"events\":[]}");
                return;
            }
            if (path.endsWith("/claim")) {
                String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(request.contains("\"namespace\":\"payments\""));
                assertTrue(request.contains("\"acknowledgeUnknownOrigin\":true"));
                respond(exchange, "{\"session\":{\"id\":\"scoped\",\"namespace\":\"payments\"}}");
                return;
            }
            assertTrue(query.contains("limit=25"));
            assertTrue(query.contains("cursor=next+token") || query.contains("cursor=next%20token"));
            respond(exchange, "{\"sessions\":[],\"nextCursor\":\"next\"}");
        });

        Map<String, Object> page = client.agentTasks().listLegacySessions(25, "next token");
        assertEquals("next", page.get("nextCursor"));
        Map<String, Object> exported = client.agentTasks().exportLegacySession(SESSION_ID, 10, 7);
        assertTrue(exported.containsKey("session"));
        Map<String, Object> claimed = client.agentTasks().claimLegacySession(
                SESSION_ID, "payments", "tab_1", true);
        assertEquals("payments", claimed.get("namespace"));
    }

    @Test
    void recoveryMethodsRejectInvalidInputBeforeNetwork() {
        assertThrows(IllegalArgumentException.class,
                () -> client.agentTasks().listLegacySessions(0, null));
        assertThrows(IllegalArgumentException.class,
                () -> client.agentTasks().exportLegacySession(SESSION_ID, 2001, 0));
        assertThrows(IllegalArgumentException.class,
                () -> client.agentTasks().exportLegacySession(SESSION_ID, 10, -1));
        assertThrows(IllegalArgumentException.class,
                () -> client.agentTasks().claimLegacySession(SESSION_ID, "payments", null, false));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
