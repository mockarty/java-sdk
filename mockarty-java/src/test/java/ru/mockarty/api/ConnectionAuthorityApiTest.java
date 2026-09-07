package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectionAuthorityApiTest {
    @Test
    void rejectsGlobalScopeAndNonPositiveRevisionBeforeNetwork() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").namespace("*")
                .timeout(Duration.ofMillis(50)).maxRetries(0).build()) {
            assertThrows(IllegalArgumentException.class,
                    () -> client.connections().getCurrent("*", "gitlab-prod"));
            assertThrows(IllegalArgumentException.class,
                    () -> client.connections().revoke("team-a", "gitlab-prod", 0));
        }
    }

    @Test
    void lifecycleUsesEscapedNamespaceExactCasAndServerOwnedIdentity() throws Exception {
        List<String> requests = new ArrayList<>();
        List<JsonNode> bodies = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            if (requestBody.length > 0) bodies.add(mapper.readTree(requestBody));
            byte[] response = "{\"frozen\":true,\"digest\":\"sha256:test\",\"descriptor\":{}}"
                    .getBytes(StandardCharsets.UTF_8);
            int status = exchange.getRequestMethod().equals("DELETE") ? 204 : 200;
            exchange.sendResponseHeaders(status, status == 204 ? -1 : response.length);
            if (status != 204) exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("mk_test").namespace("team a").maxRetries(0).build()) {
            ObjectNode descriptor = mapper.createObjectNode();
            descriptor.put("namespace", "team a");
            descriptor.put("id", "gitlab-prod");
            descriptor.put("revision", 99);
            descriptor.put("kind", "gitlab");
            client.connections().create(descriptor);
            client.connections().advance("team a", "gitlab/prod", descriptor, 1);
            client.connections().getCurrent("team a", "gitlab/prod");
            client.connections().revoke("team a", "gitlab/prod", 2);
        } finally {
            server.stop(0);
        }

        assertEquals(List.of(
                "POST /api/v1/namespaces/team%20a/connections",
                "PUT /api/v1/namespaces/team%20a/connections/gitlab%2Fprod?expectedRevision=1",
                "GET /api/v1/namespaces/team%20a/connections/gitlab%2Fprod",
                "DELETE /api/v1/namespaces/team%20a/connections/gitlab%2Fprod?revision=2"), requests);
        assertFalse(bodies.get(0).has("namespace"));
        assertFalse(bodies.get(0).has("revision"));
        assertEquals("gitlab-prod", bodies.get(0).path("id").asText());
        assertFalse(bodies.get(1).has("namespace"));
        assertFalse(bodies.get(1).has("revision"));
        assertFalse(bodies.get(1).has("id"));
    }
}
