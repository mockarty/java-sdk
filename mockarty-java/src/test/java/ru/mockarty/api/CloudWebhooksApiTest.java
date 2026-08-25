// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudWebhookCredential;
import ru.mockarty.model.CloudWebhookDelivery;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudWebhooksApiTest {
    private final List<CapturedRequest> requests = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/webhooks", this::handleWebhookRequest);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("cloud-session-token")
                .timeout(Duration.ofSeconds(5))
                .maxRetries(0)
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void lifecycleUsesCloudRoutesWorkspaceSelectorBearerAndJsonBody() {
        assertEquals("hook-1", client.cloudWebhooks().list("space/alpha").get(0).getId());

        CloudWebhookCredential created = client.cloudWebhooks().create(
                "space/alpha", "CI alerts", "https://hooks.example.test/mockarty",
                List.of("instance.ready", "subscription.changed"));
        assertEquals("whsec_create", created.getSecret());
        assertTrue(created.getWebhook().isSigningReady());

        client.cloudWebhooks().deactivate("space/alpha", "hook/1");
        client.cloudWebhooks().test("space/alpha", "hook/1");

        List<CloudWebhookDelivery> deliveries = client.cloudWebhooks()
                .listDeliveries("space/alpha", "hook/1", 25);
        assertEquals(1, deliveries.size());
        assertEquals(202, deliveries.get(0).getStatusCode());

        assertRequest(0, "GET", "/api/v1/cloud/webhooks", "workspace_id=space%2Falpha", "");
        assertRequest(1, "POST", "/api/v1/cloud/webhooks", "workspace_id=space%2Falpha", null);
        assertEquals(Map.of(
                        "name", "CI alerts",
                        "url", "https://hooks.example.test/mockarty",
                        "events", List.of("instance.ready", "subscription.changed")),
                parseJsonObject(requests.get(1).body));
        assertRequest(2, "DELETE", "/api/v1/cloud/webhooks/hook%2F1", "workspace_id=space%2Falpha", "");
        assertRequest(3, "POST", "/api/v1/cloud/webhooks/hook%2F1/test", "workspace_id=space%2Falpha", "{}");
        assertRequest(4, "GET", "/api/v1/cloud/webhooks/hook%2F1/deliveries",
                "workspace_id=space%2Falpha&limit=25", "");
    }

    @Test
    void rotationSendsStableIdempotencyKeyAndReturnsOneTimeCredential() {
        CloudWebhookCredential rotated = client.cloudWebhooks()
                .rotateSecret("space-a", "hook-1", "rotate-2026-08-23");

        assertEquals("whsec_rotated", rotated.getSecret());
        assertEquals("ready", rotated.getWebhook().getSigningStatus());
        assertRequest(0, "POST", "/api/v1/cloud/webhooks/hook-1/rotate-secret",
                "workspace_id=space-a", "{}");
        assertEquals("rotate-2026-08-23", requests.get(0).idempotencyKey);
    }

    @Test
    void invalidIdentifiersFailBeforeAnyNetworkRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudWebhooks().deactivate("space-a", " "));
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudWebhooks().test("space-a", null));
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudWebhooks().listDeliveries("space-a", "", 5));
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudWebhooks().rotateSecret("space-a", "hook-1", " "));
        assertEquals(0, requests.size());
    }

    @Test
    void deliveryLimitOutsideServerBoundsUsesSafeDefault() {
        client.cloudWebhooks().listDeliveries("space-a", "hook-1", 501);
        assertEquals("workspace_id=space-a&limit=100", requests.get(0).query);
    }

    private void assertRequest(int index, String method, String path, String query, String body) {
        CapturedRequest request = requests.get(index);
        assertEquals(method, request.method);
        assertEquals(path, request.path);
        assertEquals(query, request.query);
        if (body != null) assertEquals(body, request.body);
        assertEquals("Bearer cloud-session-token", request.authorization);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        try {
            return client.getObjectMapper().readValue(json, Map.class);
        } catch (IOException error) {
            throw new AssertionError("request body is not a JSON object", error);
        }
    }

    private void handleWebhookRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new CapturedRequest(
                exchange.getRequestMethod(), path, exchange.getRequestURI().getRawQuery(), body,
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("Idempotency-Key")));

        if (path.endsWith("/rotate-secret")) {
            reply(exchange, 200, credential("whsec_rotated"));
        } else if (path.endsWith("/deliveries")) {
            reply(exchange, 200, "{\"deliveries\":[{\"id\":\"delivery-1\",\"webhook_id\":\"hook-1\","
                    + "\"workspace_id\":\"space-a\",\"event\":\"instance.ready\",\"status\":\"success\","
                    + "\"status_code\":202,\"response_body\":\"accepted\",\"attempt\":1,"
                    + "\"last_attempt_at\":\"2026-08-23T10:00:00Z\",\"delivered_at\":\"2026-08-23T10:00:01Z\"}]}");
        } else if ("GET".equals(exchange.getRequestMethod())) {
            reply(exchange, 200, "{\"webhooks\":[" + webhookJson() + "]}");
        } else if ("POST".equals(exchange.getRequestMethod()) && !path.endsWith("/test")) {
            reply(exchange, 201, credential("whsec_create"));
        } else {
            reply(exchange, 200, "{\"status\":\"ok\"}");
        }
    }

    private static String credential(String secret) {
        return "{\"webhook\":" + webhookJson() + ",\"secret\":\"" + secret + "\"}";
    }

    private static String webhookJson() {
        return "{\"id\":\"hook-1\",\"workspace_id\":\"space-a\",\"name\":\"CI alerts\","
                + "\"url\":\"https://hooks.example.test/mockarty\",\"events\":[\"instance.ready\"],"
                + "\"active\":true,\"signing_ready\":true,\"signing_status\":\"ready\","
                + "\"created_at\":\"2026-08-23T09:00:00Z\",\"updated_at\":\"2026-08-23T09:00:00Z\"}";
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static final class CapturedRequest {
        private final String method;
        private final String path;
        private final String query;
        private final String body;
        private final String authorization;
        private final String idempotencyKey;

        private CapturedRequest(String method, String path, String query, String body,
                                String authorization, String idempotencyKey) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.body = body;
            this.authorization = authorization;
            this.idempotencyKey = idempotencyKey;
        }
    }
}
