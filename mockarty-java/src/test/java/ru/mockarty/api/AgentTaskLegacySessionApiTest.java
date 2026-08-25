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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentTaskLegacySessionApiTest {
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/agent/sessions/legacy", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/legacy-1/claim")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(body.contains("\"namespace\":\"payments\""));
                assertTrue(body.contains("\"acknowledgeUnknownOrigin\":true"));
                reply(exchange, "{\"session\":{\"id\":\"session-1\",\"namespace\":\"payments\"}}");
                return;
            }
            assertEquals("limit=10&cursor=next%2Fvalue", exchange.getRequestURI().getRawQuery());
            reply(exchange, "{\"sessions\":[{\"originalId\":\"legacy-1\"}],\"nextCursor\":\"next\"}");
        });
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("key").timeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test void listAndClaimLegacySession() throws Exception {
        Map<String, Object> page = client.agentTasks().listLegacySessions(10, "next/value");
        assertEquals("next", page.get("nextCursor"));
        Map<String, Object> session = client.agentTasks().claimLegacySession(
                "legacy-1", "payments", null, true);
        assertEquals("session-1", session.get("id"));
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
