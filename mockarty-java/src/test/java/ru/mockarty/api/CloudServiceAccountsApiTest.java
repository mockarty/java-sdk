package ru.mockarty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudServiceAccountsApiTest {
    private final List<String> requests = new ArrayList<>();
    private final List<String> idempotencyKeys = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("mk_test").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void managementAndMachineWorkloadUseExactRoutesAndHeaders() {
        var management = client.cloudServiceAccounts();
        management.list("space-a");
        management.create("space-a", "ci", List.of("space.read"), false, 0, "create-1");
        var machine = client.cloudServiceAccount();
        assertEquals("service_account", machine.self().getCredentialKind());
        var body = new ObjectMapper().createObjectNode().put("version", 1);
        machine.createProject("space-a", "A", body, "project-create-1");
        machine.dispatchRun("space-a", "2030-01-01T00:00:00Z", "execute", "batch",
                "11111111-1111-4111-8111-111111111111", body, "run-dispatch-1");
        assertEquals(List.of(
                "GET /api/v1/cloud/spaces/space-a/service-accounts",
                "POST /api/v1/cloud/spaces/space-a/service-accounts",
                "GET /api/v1/cloud/service-account/self",
                "POST /api/v1/cloud/service-account/spaces/space-a/shared/projects",
                "POST /api/v1/cloud/service-account/spaces/space-a/shared/runs"), requests);
        assertEquals(List.of("create-1", "project-create-1", "run-dispatch-1"), idempotencyKeys);
        assertTrue(requests.get(3).contains("/service-account/spaces/"));
    }

    @Test void mutationRefusesMissingIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudServiceAccounts()
                .create("space-a", "ci", List.of("space.read"), false, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> client.cloudServiceAccount()
                .createProject("space-a", "A", new ObjectMapper().createObjectNode(), ""));
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        String key = exchange.getRequestHeaders().getFirst("Idempotency-Key");
        if (key != null) idempotencyKeys.add(key);
        String value;
        if (exchange.getRequestURI().getPath().endsWith("/self")) {
            value = "{\"credential_kind\":\"service_account\",\"service_account\":{\"id\":\"sa\"}}";
        } else if (exchange.getRequestURI().getPath().endsWith("/service-accounts") && exchange.getRequestMethod().equals("GET")) {
            value = "{\"service_accounts\":[],\"capacity\":{\"remaining\":1}}";
        } else if (exchange.getRequestURI().getPath().contains("/shared/projects")) {
            value = "{\"id\":\"p1\",\"name\":\"A\",\"body\":{},\"revision\":1}";
        } else if (exchange.getRequestURI().getPath().contains("/shared/runs")) {
            value = "{\"id\":\"11111111-1111-4111-8111-111111111111\",\"state\":\"accepted\",\"revision\":1}";
        } else {
            value = "{\"service_account\":{\"id\":\"sa\"}}";
        }
        byte[] response = value.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
