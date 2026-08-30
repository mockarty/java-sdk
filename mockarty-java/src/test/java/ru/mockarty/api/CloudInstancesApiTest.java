package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudInstanceCreateResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudInstancesApiTest {
    private final List<Captured> requests = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/instances", this::handle);
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
    void lifecycleUsesExplicitSpaceAndIdempotencyHeaders() {
        CloudInstanceCreateResult created = client.cloudInstances().create("space-1", "Managed", "create-1");
        assertEquals("instance-1", created.getInstance().getId());
        assertEquals("one-time", created.getBootstrap().getPassword());
        client.cloudInstances().list("space-1");
        client.cloudInstances().get("instance/1");
        client.cloudInstances().stop("instance/1", "stop-1");
        client.cloudInstances().start("instance/1", "start-1");
        client.cloudInstances().delete("instance/1", "delete-1");

        assertEquals("create-1", requests.get(0).key);
        assertEquals("workspace_id=space-1", requests.get(1).query);
        assertTrue(requests.get(2).path.endsWith("instance%2F1"));
        assertEquals("stop-1", requests.get(3).key);
        assertEquals("DELETE", requests.get(5).method);
    }

    @Test
    void emptyRetryIdentityFailsBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudInstances().create("space-1", "Managed", " "));
        assertThrows(IllegalArgumentException.class, () -> client.cloudInstances().delete("instance-1", ""));
        assertEquals(0, requests.size());
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(new Captured(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders().getFirst("Idempotency-Key")));
        String body = exchange.getRequestMethod().equals("POST") && exchange.getRequestURI().getRawPath().equals("/api/v1/cloud/instances")
                ? "{\"instance\":{\"id\":\"instance-1\"},\"bootstrap\":{\"available\":true,\"password\":\"one-time\",\"one_time\":true}}"
                : exchange.getRequestMethod().equals("GET") && exchange.getRequestURI().getRawPath().equals("/api/v1/cloud/instances")
                ? "{\"instances\":[],\"total\":0}" : "{\"instance\":{\"id\":\"instance-1\"}}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static final class Captured {
        private final String method;
        private final String path;
        private final String query;
        private final String key;

        private Captured(String method, String path, String query, String key) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.key = key;
        }
    }
}
