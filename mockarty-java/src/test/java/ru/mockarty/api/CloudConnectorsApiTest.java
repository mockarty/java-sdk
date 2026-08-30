package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudConnector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudConnectorsApiTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String idempotencyKey;
    private volatile String requestBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/operator/connectors", this::handle);
        server.createContext("/api/v1/cloud/operator/connector-versions", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("operator-session").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void updateUsesWriteOnlyClientSecretAndRetryIdentity() {
        CloudConnector connector = client.cloudConnectors().update("oauth", "github", "",
                Map.of("client_id", "client"), Map.of("client_secret", "write-only"),
                Collections.emptyList(), 1, true, false, "connector-1");
        assertEquals("oauth/github", connector.getKey());
        assertEquals(2, connector.getRevision());
        assertEquals("connector-1", idempotencyKey);
        assertFalse(requestBody.contains("secret_configured\":\"write-only"));
    }

    @Test
    void invalidMutationFailsBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudConnectors().update(
                "payment", "stripe", "", Map.of(), Map.of(), Collections.emptyList(),
                1, false, false, "key"));
        assertThrows(IllegalArgumentException.class, () -> client.cloudConnectors().update(
                "oauth", "github", "", Map.of(), Map.of(), Collections.emptyList(),
                0, false, false, "key"));
    }

    @Test
    void revokeAcceptsEmptyNoContentResponse() {
        client.cloudConnectors().revoke("8bb0c85e-508b-4d83-b7c7-b8b87c910ecd", "revoke-1");
        assertEquals("revoke-1", idempotencyKey);
    }

    private void handle(HttpExchange exchange) throws IOException {
        idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (exchange.getRequestURI().getRawPath().endsWith("/revoke")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        byte[] bytes = "{\"key\":\"oauth/github\",\"revision\":2,\"secret_configured\":true}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
