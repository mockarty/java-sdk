// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
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

class WorkflowDefinitionsApiTest {
    private final List<Request> requests = new ArrayList<>();
    private MockartyClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/namespaces", this::handle);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("key")
                .namespace("team-a")
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
    void lifecycleUsesExactTenantVersionAndRevisionBody() throws Exception {
        JsonNode definition = client.getObjectMapper().readTree("{\"contractVersion\":\"mockarty.workflow/v1\","
                + "\"namespace\":\"team-a\",\"id\":\"release-flow\",\"version\":\"1.0.0\","
                + "\"status\":\"draft\",\"entryNode\":\"start\",\"nodes\":[],\"transitions\":[]}");
        assertEquals(1, client.workflowDefinitions().createDraft(definition).path("revision").asInt());
        assertEquals(true, client.workflowDefinitions().dryRun("team-a", "release-flow", "1.0.0", 1).path("ready").asBoolean());
        assertEquals(2, client.workflowDefinitions().publish("team-a", "release-flow", "1.0.0", 1).path("revision").asInt());
        client.workflowDefinitions().list("team-a", "", "published", "", 25);

        assertRequest(0, "POST", "/api/v1/namespaces/team-a/workflow-definitions", null);
        assertRequest(1, "POST", "/api/v1/namespaces/team-a/workflow-definitions/release-flow/versions/1.0.0/dry-run", null);
        assertEquals(1, client.getObjectMapper().readTree(requests.get(1).body).path("expectedRevision").asLong());
        assertRequest(2, "POST", "/api/v1/namespaces/team-a/workflow-definitions/release-flow/versions/1.0.0/publish", null);
        assertRequest(3, "GET", "/api/v1/namespaces/team-a/workflow-definitions", "status=published&limit=25");
    }

    @Test
    void invalidIdentityAndRevisionFailBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.workflowDefinitions().list("*", "", "", "", 10));
        assertThrows(IllegalArgumentException.class, () -> client.workflowDefinitions().get("team-a", "", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> client.workflowDefinitions().publish("team-a", "flow", "1.0.0", 0));
        assertEquals(0, requests.size());
    }

    @Test
    void createDraftCopiesClientNamespaceWithoutMutatingInput() throws Exception {
        JsonNode definition = client.getObjectMapper().readTree("{\"contractVersion\":\"mockarty.workflow/v1\","
                + "\"id\":\"release-flow\",\"version\":\"1.0.0\",\"status\":\"draft\","
                + "\"entryNode\":\"start\",\"nodes\":[],\"transitions\":[]}");
        client.workflowDefinitions().createDraft(definition);
        assertRequest(0, "POST", "/api/v1/namespaces/team-a/workflow-definitions", null);
        assertEquals("team-a", client.getObjectMapper().readTree(requests.get(0).body).path("namespace").asText());
        assertEquals(false, definition.has("namespace"));
    }

    private void assertRequest(int index, String method, String path, String query) {
        Request request = requests.get(index);
        assertEquals(method, request.method);
        assertEquals(path, request.path);
        assertEquals(query, request.query);
        assertEquals("Bearer key", request.authorization);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders().getFirst("Authorization"), body));
        String response = "{}";
        if (requests.size() == 1) response = "{\"revision\":1}";
        if (exchange.getRequestURI().getPath().endsWith("/dry-run")) response = "{\"ready\":true}";
        if (exchange.getRequestURI().getPath().endsWith("/publish")) response = "{\"revision\":2}";
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class Request {
        private final String method;
        private final String path;
        private final String query;
        private final String authorization;
        private final String body;

        private Request(String method, String path, String query, String authorization, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.authorization = authorization;
            this.body = body;
        }
    }
}
