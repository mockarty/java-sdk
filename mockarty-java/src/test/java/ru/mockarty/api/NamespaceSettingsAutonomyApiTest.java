package ru.mockarty.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.AutonomyNamespaceSettings;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamespaceSettingsAutonomyApiTest {
    @Test
    void savesAndReadsRetention() throws Exception {
        List<String> putBodies = new ArrayList<>();
        List<String> idempotencyKeys = new ArrayList<>();
        List<String> ifMatchValues = new ArrayList<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/autotester/settings", exchange -> {
            if ("PUT".equals(exchange.getRequestMethod())) {
                putBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                idempotencyKeys.add(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
                ifMatchValues.add(exchange.getRequestHeaders().getFirst("If-Match"));
            }
            byte[] body = ("{\"defaultAutonomy\":\"auto\",\"defaultBudget\":{\"tokensTotal\":100},\"defaultContextRefs\":[{\"kind\":\"spec\",\"value\":\"openapi.yaml\"}],\"journalEventRetentionDays\":365,\"journalPayloadRetentionDays\":30,\"runWindowMinutes\":480," +
                    "\"updatedAt\":\"2026-08-23T00:00:00Z\"," +
                    "\"etag\":\"\\\"" + "a".repeat(64) + "\\\"\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try (MockartyClient client = MockartyClient.create("http://127.0.0.1:" + server.getAddress().getPort())) {
            AutonomyNamespaceSettings saved = client.namespaceSettings().saveAutonomySettings(
                    new AutonomyNamespaceSettings().journalEventRetentionDays(365), "stable-java-save-1");
            assertEquals(365, saved.getJournalEventRetentionDays());
            assertEquals(30, client.namespaceSettings().getAutonomySettings().getJournalPayloadRetentionDays());
            assertTrue(putBodies.get(0).contains("\"defaultAutonomy\":\"auto\""));
            assertTrue(putBodies.get(0).contains("\"tokensTotal\":100"));
            assertTrue(putBodies.get(0).contains("openapi.yaml"));
            assertTrue(putBodies.get(0).contains("\"journalPayloadRetentionDays\":30"));
			assertTrue(putBodies.get(0).contains("\"runWindowMinutes\":480"));
            assertEquals("stable-java-save-1", idempotencyKeys.get(0));
            assertEquals("\"" + "a".repeat(64) + "\"", ifMatchValues.get(0));

            client.namespaceSettings().clearAutonomyRetention(false, true, "stable-java-clear-1");
            String clearBody = putBodies.get(1);
            assertTrue(clearBody.contains("\"journalPayloadRetentionDays\":null"));
            assertTrue(clearBody.contains("\"journalEventRetentionDays\":365"));
            assertEquals("stable-java-clear-1", idempotencyKeys.get(1));
            assertThrows(IllegalArgumentException.class,
                    () -> client.namespaceSettings().clearAutonomyRetention(false, false));
			client.namespaceSettings().clearAutonomyRunWindow("stable-window-clear-1");
			assertTrue(putBodies.get(2).contains("\"runWindowMinutes\":null"));
        } finally {
            server.stop(0);
        }
    }
}
