// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudSpacesApiTest {
    private final List<Captured> requests = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/cloud", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("cloud-token").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    @Test void explicitSpaceRoutesAndMutationHeaders() {
        client.cloudSpaces().list("next", 25);
        client.cloudSpaces().get("s1");
        client.cloudSpaces().listMembers("s1", "members-next", 25);
        client.cloudSpaces().listInvites("s1", "invites-next", 25);
        client.cloudSpaces().previewInvite("token/one");
        client.cloudSpaces().createInvite("s1", "new@example.test", "viewer", 0,
                "\"space-s1-r7\"", "retry-1");
        client.cloudSpaces().revokeInvite("s1", "i1", "\"space-s1-r7\"", "retry-1");
        client.cloudSpaces().acceptInvite("token/one", "\"space-s1-r7\"", "retry-1");
        client.cloudSpaces().updateMemberRole("s1", "u1", "editor", "\"space-s1-r7\"", "retry-1");
        client.cloudSpaces().removeMember("s1", "u1", "\"space-s1-r7\"", "retry-1");
        List<String> expected = List.of(
                "GET /api/v1/cloud/spaces?cursor=next&limit=25",
                "GET /api/v1/cloud/spaces/s1",
                "GET /api/v1/cloud/spaces/s1/members?cursor=members-next&limit=25",
                "GET /api/v1/cloud/spaces/s1/invites?cursor=invites-next&limit=25",
                "GET /api/v1/cloud/invites/token%2Fone",
                "POST /api/v1/cloud/spaces/s1/invites",
                "DELETE /api/v1/cloud/spaces/s1/invites/i1",
                "POST /api/v1/cloud/invites/token%2Fone/accept",
                "PATCH /api/v1/cloud/spaces/s1/members/u1",
                "DELETE /api/v1/cloud/spaces/s1/members/u1");
        assertEquals(expected.size(), requests.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), requests.get(i).methodPath);
        }
        for (int i = 5; i < requests.size(); i++) {
            assertEquals("\"space-s1-r7\"", requests.get(i).ifMatch);
            assertEquals("retry-1", requests.get(i).idempotencyKey);
        }
    }

    @Test void missingPreconditionsFailBeforeNetwork() {
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudSpaces().removeMember("s1", "u1", "", ""));
        assertEquals(0, requests.size());
    }

    @Test void nonPositiveLimitUsesServerDefault() {
        client.cloudSpaces().list("", 0);
        client.cloudSpaces().listMembers("s1", "next", -1);
        assertEquals("GET /api/v1/cloud/spaces", requests.get(0).methodPath);
        assertEquals("GET /api/v1/cloud/spaces/s1/members?cursor=next", requests.get(1).methodPath);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(new Captured(exchange.getRequestMethod() + " " + exchange.getRequestURI(),
                exchange.getRequestHeaders().getFirst("If-Match"),
                exchange.getRequestHeaders().getFirst("Idempotency-Key")));
        byte[] body = "{\"items\":[],\"status\":\"ok\",\"revision\":8}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static final class Captured {
        private final String methodPath;
        private final String ifMatch;
        private final String idempotencyKey;

        private Captured(String methodPath, String ifMatch, String idempotencyKey) {
            this.methodPath = methodPath;
            this.ifMatch = ifMatch;
            this.idempotencyKey = idempotencyKey;
        }
    }
}
