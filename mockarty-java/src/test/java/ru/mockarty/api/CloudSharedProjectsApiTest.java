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
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudSharedProjectsApiTest {
    private final List<String> requests = new ArrayList<>();
    private final List<String> requestIds = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud/spaces", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("mk_test").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void crudUsesOnlyPublicSpaceScopedProxy() {
        var api = client.cloudSharedProjects();
        assertTrue(api.list("space-a", "", 50).getProjects().isEmpty());
        var body = new ObjectMapper().createObjectNode().put("version", 1);
        String createRequestId = "11111111-1111-4111-8111-111111111111";
        assertEquals(1, api.create("space-a", "A", body, createRequestId).getRevision());
        api.update("space-a", "project-a", "B", body, 1);
        api.delete("space-a", "project-a", 2);
        assertEquals(List.of(
                "GET /api/v1/cloud/spaces/space-a/shared/projects?limit=50",
                "POST /api/v1/cloud/spaces/space-a/shared/projects",
                "PUT /api/v1/cloud/spaces/space-a/shared/projects/project-a",
                "DELETE /api/v1/cloud/spaces/space-a/shared/projects/project-a?revision=2"), requests);
        assertEquals(3, requestIds.size());
        assertEquals(createRequestId, requestIds.get(0));
        assertTrue(requestIds.stream().allMatch(value -> value != null && !value.isBlank()));
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
        if (!exchange.getRequestMethod().equals("GET")) {
            requestIds.add(exchange.getRequestHeaders().getFirst("X-Request-ID"));
        }
        byte[] response = (exchange.getRequestMethod().equals("GET") && exchange.getRequestURI().getPath().endsWith("projects"))
                ? "{\"projects\":[],\"next_cursor\":\"\",\"has_more\":false}".getBytes(StandardCharsets.UTF_8)
                : "{\"id\":\"project-a\",\"name\":\"A\",\"body\":{},\"revision\":1}".getBytes(StandardCharsets.UTF_8);
        if (exchange.getRequestMethod().equals("DELETE")) { exchange.sendResponseHeaders(204, -1); }
        else { exchange.getResponseHeaders().set("Content-Type", "application/json"); exchange.sendResponseHeaders(200, response.length); exchange.getResponseBody().write(response); }
        exchange.close();
    }
}
