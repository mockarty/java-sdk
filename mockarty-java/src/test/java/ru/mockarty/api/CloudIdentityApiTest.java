package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudIdentityApiTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String cookie;
    private volatile String idempotencyKey;
    private final List<String> emailPaths = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/auth", this::handle);
        server.createContext("/api/v1/cloud/email", this::handleEmail);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("cloud-session").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void stepUpCookieIsUsedForUnlink() {
        assertEquals("verified", client.cloudIdentity().stepUp("oauth_identity_unlink", "current", true).getStatus());
        client.cloudIdentity().unlink("github", "unlink-1");
        assertTrue(cookie.contains("mockarty_cloud_step_up=proof"));
        assertEquals("unlink-1", idempotencyKey);
        assertTrue(client.cloudIdentity().linkUrl("github").endsWith("/api/v1/cloud/auth/oauth/github/link"));
    }

    @Test
    void emailChangeUsesTwoDistinctEndpoints() {
        assertEquals("awaiting_current_email", client.cloudIdentity()
                .requestEmailChange("new@example.test", "", "").getStage());
        client.cloudIdentity().authorizeCurrentEmail("old-proof");
        client.cloudIdentity().confirmNewEmail("new-proof");
        client.cloudIdentity().cancelEmailChange();
        assertEquals(List.of("PATCH /api/v1/cloud/email", "POST /api/v1/cloud/auth/email/change/authorize",
                "POST /api/v1/cloud/auth/email/change/confirm", "DELETE /api/v1/cloud/email/pending"), emailPaths);
    }

    private void handleEmail(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        emailPaths.add(exchange.getRequestMethod() + " " + path);
        byte[] response = (path.endsWith("/pending") ? "{\"status\":\"cancelled\"}" :
                "{\"status\":\"verification_pending\",\"stage\":\"awaiting_current_email\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(response); }
    }

    private void handle(HttpExchange exchange) throws IOException {
        byte[] response = new byte[0];
        int status = 204;
        if (exchange.getRequestURI().getPath().endsWith("/step-up")) {
            exchange.getResponseHeaders().add("Set-Cookie", "mockarty_cloud_step_up=proof; Path=/api/v1/cloud; HttpOnly");
            response = "{\"status\":\"verified\",\"action\":\"oauth_identity_unlink\"}".getBytes(StandardCharsets.UTF_8);
            status = 200;
        } else if (exchange.getRequestURI().getPath().contains("/email/change/")) {
            emailPaths.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            response = "{\"status\":\"verification_pending\",\"stage\":\"awaiting_new_email\"}"
                    .getBytes(StandardCharsets.UTF_8);
            status = 200;
        } else {
            cookie = exchange.getRequestHeaders().getFirst("Cookie");
            idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        }
        exchange.sendResponseHeaders(status, status == 204 ? -1 : response.length);
        if (response.length > 0) try (OutputStream output = exchange.getResponseBody()) { output.write(response); }
    }
}
