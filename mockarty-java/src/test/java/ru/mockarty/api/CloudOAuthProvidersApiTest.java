package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudOAuthProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudOAuthProvidersApiTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String idempotencyKey;
    private volatile String requestBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/operator/oauth/providers", this::handle);
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
    void updateUsesWriteOnlyReferenceAndRetryIdentity() {
        CloudOAuthProvider provider = client.cloudOAuthProviders().update("github", "client-id",
                "env://CLOUD_API_PROVIDER_SECRET_OAUTH_GITHUB", 3, true, "oauth-provider-4");
        assertEquals("github", provider.getProvider());
        assertEquals(4, provider.getConfigRevision());
        assertEquals("oauth-provider-4", idempotencyKey);
        assertFalse(requestBody.contains("raw-secret"));
        assertEquals(1, client.cloudOAuthProviders().list().size());
    }

    @Test
    void invalidMutationFailsBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudOAuthProviders()
                .update("github", "client-id", "", -1, false, "key"));
        assertThrows(IllegalArgumentException.class, () -> client.cloudOAuthProviders()
                .update("", "client-id", "", 0, false, "key"));
    }

    private void handle(HttpExchange exchange) throws IOException {
        idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String body = exchange.getRequestURI().getRawPath().endsWith("/github")
                ? "{\"provider\":\"github\",\"client_id\":\"client-id\",\"source\":\"registry\",\"config_revision\":4,\"enabled\":true,\"secret_configured\":true}"
                : "{\"providers\":[{\"provider\":\"github\",\"config_revision\":4,\"enabled\":true,\"secret_configured\":true}]}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
