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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Device-mesh wire contract.
 *
 * <p>The responses below are LITERAL server JSON (camelCase, as the routes emit
 * it): a test that round-trips the SDK's own model agrees with itself whatever
 * the server sends, which is how a naming mismatch shipped once already.
 */
class DeviceMeshApiTest {

    private final List<Request> requests = new ArrayList<>();
    private MockartyClient client;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1", this::handle);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("mk_writer")
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
    void listDevicesParsesTheServedShape() throws Exception {
        var devices = client.deviceMesh().listDevices("");

        assertEquals(1, devices.size());
        assertEquals("desk-1", devices.get(0).getDeviceId());
        assertEquals("online", devices.get(0).getState());
        assertEquals(List.of("api_test"), devices.get(0).getCapabilities());
        assertEquals(Map.of("os", "linux"), devices.get(0).getLabels());
        assertEquals("/api/v1/namespaces/team-a/device-mesh/devices", requests.get(0).path);
    }

    @Test
    void submitCarriesTheIdempotencyKeyAndReportsReplay() throws Exception {
        var result = client.deviceMesh().submit("team-a", "api_test.collection",
                Map.of("collectionId", "11111111-1111-4111-8111-111111111111"),
                "ci-run-42", "desk-1", List.of("api_test"), null);

        assertTrue(result.isReplayed(), "a replayed key must be reported, not hidden");
        assertEquals("cmd-1", result.getCommand().getId());
        JsonNode body = client.getObjectMapper().readTree(requests.get(0).body);
        assertEquals("ci-run-42", body.path("idempotencyKey").asText());
        assertEquals("desk-1", body.path("targetDeviceId").asText());
        assertEquals("api_test", body.path("requiredCapabilities").get(0).asText());
    }

    @Test
    void reportSendsTheDeviceAsAQueryParameter() throws Exception {
        var command = client.deviceMesh().report("team-a", "desk-1", "cmd-1", "failed",
                "0 of 3 passed", null, "engine exited 2");

        assertEquals("/api/v1/namespaces/team-a/device-mesh/commands/cmd-1/report", requests.get(0).path);
        assertEquals("deviceId=desk-1", requests.get(0).query);
        assertEquals("failed", command.getState());
        assertEquals("engine exited 2", command.getFailureReason());
    }

    @Test
    void receiptsAreReadByCommand() throws Exception {
        var reports = client.deviceMesh().listReports("team-a", "cmd-1");

        assertEquals("/api/v1/namespaces/team-a/device-mesh/reports", requests.get(0).path);
        assertEquals("commandId=cmd-1", requests.get(0).query);
        assertEquals(1, reports.size());
        assertEquals("engine exited 2", reports.get(0).getFailureCode());
        assertEquals("devicemesh.command:cmd-1", reports.get(0).getRef());
    }

    @Test
    void drainSendsTheFlagAndReturnsTheState() throws Exception {
        var device = client.deviceMesh().setDrain("team-a", "desk-1", true, "maintenance");

        assertEquals("draining", device.getState());
        JsonNode body = client.getObjectMapper().readTree(requests.get(0).body);
        assertTrue(body.path("draining").asBoolean());
        assertEquals("maintenance", body.path("reason").asText());
    }

    @Test
    void grantsLendAndWithdraw() throws Exception {
        var grant = client.deviceMesh().createGrant("team-a", "desk-1", "user-2", List.of("api_test"));
        assertEquals("user-2", grant.getGranteeUserId());

        client.deviceMesh().revokeGrant("team-a", "desk-1", "user-2");
        assertEquals("DELETE", requests.get(1).method);
        assertEquals("/api/v1/namespaces/team-a/device-mesh/devices/desk-1/grants/user-2", requests.get(1).path);
    }

    @Test
    void missingNamespaceOrFieldsFailBeforeNetwork() {
        MockartyClient bare = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_writer").namespace("").build();
        try {
            assertThrows(IllegalArgumentException.class, () -> bare.deviceMesh().listDevices(""));
            assertThrows(IllegalArgumentException.class,
                    () -> client.deviceMesh().submit("team-a", "ping", null, " ", null, null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> client.deviceMesh().setDrain("team-a", " ", true, null));
            assertEquals(0, requests.size());
        } finally {
            bare.close();
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(),
                exchange.getRequestHeaders().getFirst("Authorization"), body));
        String path = exchange.getRequestURI().getPath();
        String response;
        int status = 200;
        if (path.endsWith("/devices")) {
            response = "{\"devices\":[{\"deviceId\":\"desk-1\",\"namespace\":\"team-a\",\"kind\":\"desktop\","
                    + "\"state\":\"online\",\"ownerUserId\":\"user-1\",\"capabilities\":[\"api_test\"],"
                    + "\"labels\":{\"os\":\"linux\"},\"heartbeatAt\":\"2026-10-27T10:00:00Z\"}]}";
        } else if (path.endsWith("/grants")) {
            response = "{\"deviceId\":\"desk-1\",\"namespace\":\"team-a\",\"granteeUserId\":\"user-2\","
                    + "\"capabilities\":[\"api_test\"],\"createdAt\":\"2026-10-27T10:00:00Z\"}";
        } else if (path.endsWith("/drain")) {
            response = "{\"deviceId\":\"desk-1\",\"namespace\":\"team-a\",\"kind\":\"desktop\","
                    + "\"state\":\"draining\",\"capabilities\":[\"api_test\"]}";
        } else if (path.endsWith("/commands")) {
            status = 202;
            response = "{\"command\":{\"id\":\"cmd-1\",\"namespace\":\"team-a\",\"kind\":\"api_test.collection\","
                    + "\"state\":\"queued\",\"targetDeviceId\":\"desk-1\","
                    + "\"requiredCapabilities\":[\"api_test\"],\"queuedAt\":\"2026-10-27T10:00:00Z\","
                    + "\"payload\":{\"collectionId\":\"11111111-1111-4111-8111-111111111111\"}},"
                    + "\"replayed\":true}";
        } else if (path.endsWith("/report")) {
            response = "{\"id\":\"cmd-1\",\"namespace\":\"team-a\",\"kind\":\"api_test.collection\","
                    + "\"state\":\"failed\",\"failureReason\":\"engine exited 2\","
                    + "\"resultSummary\":\"0 of 3 passed\"}";
        } else if (path.endsWith("/reports")) {
            response = "{\"reports\":[{\"id\":\"rep-1\",\"ref\":\"devicemesh.command:cmd-1\","
                    + "\"namespace\":\"team-a\",\"deviceId\":\"desk-1\",\"submitterUserId\":\"user-2\","
                    + "\"state\":\"failed\",\"summary\":\"0 of 3 passed\","
                    + "\"failureCode\":\"engine exited 2\",\"recordedAt\":\"2026-10-27T10:00:00Z\"}]}";
        } else {
            response = "{}";
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static final class Request {
        final String method;
        final String path;
        final String query;
        final String authorization;
        final String body;

        Request(String method, String path, String query, String authorization, String body) {
            this.method = method;
            this.path = path;
            this.query = query;
            this.authorization = authorization;
            this.body = body;
        }
    }
}
