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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudRiskApiTest {
    private HttpServer server;
    private MockartyClient client;
    private volatile String releaseBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/operator/risk/cases", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("operator-risk-token").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) client.close();
        if (server != null) server.stop(0);
    }

    @Test
    void listsReadsAndReleasesWithRevisionFence() {
        assertEquals("case-1", client.cloudRisk().listCases("open", 25).get(0).path("id").asText());
        assertEquals("case-1", client.cloudRisk().getCase("case-1").path("case").path("id").asText());
        assertEquals("released", client.cloudRisk().releaseEnforcement("case-1", "enf-1", 2, "customer verified")
                .path("enforcement").path("status").asText());
        assertTrue(releaseBody.contains("\"revision\":2"));
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getRawPath();
        String body;
        if (path.endsWith("/enforcements/enf-1/release")) {
            releaseBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            body = "{\"enforcement\":{\"id\":\"enf-1\",\"status\":\"released\",\"revision\":3}}";
        } else if (path.endsWith("/case-1")) {
            body = "{\"case\":{\"id\":\"case-1\"},\"events\":[],\"enforcements\":[]}";
        } else {
            body = "{\"cases\":[{\"id\":\"case-1\",\"status\":\"open\"}]}";
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }
}
