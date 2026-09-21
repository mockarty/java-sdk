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
        client.cloudSpaces().organization("s1");
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
                "GET /api/v1/cloud/organization?space_id=s1",
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
        for (int i = 6; i < requests.size(); i++) {
            assertEquals("\"space-s1-r7\"", requests.get(i).ifMatch);
            assertEquals("retry-1", requests.get(i).idempotencyKey);
        }
        client.cloudSpaces().revokeTeamInvite("s1", "ti1", "team-retry");
        Captured teamRevoke = requests.get(requests.size() - 1);
        assertEquals("DELETE /api/v1/cloud/team/invites/ti1?space_id=s1", teamRevoke.methodPath);
        assertEquals("team-retry", teamRevoke.idempotencyKey);
        assertEquals(null, teamRevoke.ifMatch);
    }

    @Test void missingPreconditionsFailBeforeNetwork() {
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudSpaces().removeMember("s1", "u1", "", ""));
        assertEquals(0, requests.size());
        assertThrows(IllegalArgumentException.class,
                () -> client.cloudSpaces().revokeTeamInvite("s1", "ti1", ""));
        assertEquals(0, requests.size());
    }

    @Test void nonPositiveLimitUsesServerDefault() {
        client.cloudSpaces().list("", 0);
        client.cloudSpaces().listMembers("s1", "next", -1);
        assertEquals("GET /api/v1/cloud/spaces", requests.get(0).methodPath);
        assertEquals("GET /api/v1/cloud/spaces/s1/members?cursor=next", requests.get(1).methodPath);
    }

    @Test void teamReadReportsUseExplicitSpace() {
        assertThrows(IllegalArgumentException.class, () -> client.cloudSpaces().listTeamPackages(" "));
        assertThrows(IllegalArgumentException.class, () -> client.cloudSpaces().getTeamBilling(""));
        assertEquals(0, requests.size());

        assertEquals(5, client.cloudSpaces().listTeamPackages("personal-1").get(0).get("seats"));
        assertEquals(5, client.cloudSpaces().getTeamBilling("team-space-1").get("package_seats"));
        assertEquals("GET /api/v1/cloud/team/packages?space_id=personal-1", requests.get(0).methodPath);
        assertEquals("GET /api/v1/cloud/team/billing?space_id=team-space-1", requests.get(1).methodPath);
        assertEquals(null, requests.get(0).idempotencyKey);
        assertEquals(null, requests.get(1).ifMatch);
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(new Captured(exchange.getRequestMethod() + " " + exchange.getRequestURI(),
                exchange.getRequestHeaders().getFirst("If-Match"),
                exchange.getRequestHeaders().getFirst("Idempotency-Key")));
        if (exchange.getRequestURI().getPath().startsWith("/api/v1/cloud/team/invites/")) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        String response = switch (exchange.getRequestURI().getPath()) {
            case "/api/v1/cloud/team/packages" -> "{\"packages\":[{\"seats\":5}]}";
            case "/api/v1/cloud/team/billing" -> "{\"billing\":{\"package_seats\":5}}";
            default -> "{\"items\":[],\"status\":\"ok\",\"revision\":8}";
        };
        byte[] body = response.getBytes(StandardCharsets.UTF_8);
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
