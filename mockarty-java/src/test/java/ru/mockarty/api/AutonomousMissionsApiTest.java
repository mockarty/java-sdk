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
import ru.mockarty.model.AutonomousMissionSubmitRequest;
import ru.mockarty.model.MissionEffectiveSettingsOptions;
import ru.mockarty.model.MissionStartRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMissionsApiTest {
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
    void submitListGetAndFlowUseExactWireContract() throws Exception {
        var accepted = client.autonomousMissions().submit(new AutonomousMissionSubmitRequest()
                .goal(" verify checkout ").productUrl("https://shop.example").autonomy("auto")
                .budget(12000, 0, 0));
        assertEquals("m-1", accepted.getMissionId());
        assertEquals(1, client.autonomousMissions().list("active", 25).getTotal());
        assertEquals("m-1", client.autonomousMissions().get("m-1").getId());
        assertEquals("done", client.autonomousMissions().getFlow("m-1").getMission().getStatus());

        assertEquals("POST", requests.get(0).method);
        assertEquals("/api/v1/autotester/intents", requests.get(0).path);
        JsonNode body = client.getObjectMapper().readTree(requests.get(0).body);
        assertEquals("verify checkout", body.path("goal").asText());
        assertEquals("https://shop.example", body.path("productUrl").asText());
        assertEquals(12000, body.path("budget").path("tokens_total").asLong());
        assertEquals("status=active&limit=25", requests.get(1).query);
        assertTrue(requests.stream().allMatch(r -> "Bearer mk_writer".equals(r.authorization)));
    }

    @Test
    void invalidInputFailsBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.autonomousMissions().submit(new AutonomousMissionSubmitRequest()));
        assertThrows(IllegalArgumentException.class, () -> client.autonomousMissions().submit(new AutonomousMissionSubmitRequest().goal("x").autonomy("root")));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(0, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(0, 0, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> new AutonomousMissionSubmitRequest().budget(0, 0, Double.NEGATIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> client.autonomousMissions().get(" "));
        assertThrows(IllegalArgumentException.class, () -> client.autonomousMissions().list("", 201));
        assertEquals(0, requests.size());
    }

    @Test
    void effectiveSettingsAndUnifiedStartUseDigestFence() throws Exception {
        String digest = "sha256:" + "a".repeat(64);
        var settings = client.autonomousMissions().getEffectiveSettings(
                new MissionEffectiveSettingsOptions().productId("product/checkout").runWindowMinutes(90));
        assertEquals(digest, settings.getSettingsDigest());
        assertTrue(settings.getSettings().get(0).isRuntimeApplied());

        var started = client.autonomousMissions().start(new MissionStartRequest()
                .goal(" ship checkout ").productId("product/checkout").kind("testing")
                .expectedSettingsDigest(digest));
        assertTrue(started.isCreated());
        assertEquals("m-unified", started.getMission().getId());

        assertEquals("/api/v1/missions/settings/effective", requests.get(0).path);
        assertEquals("productId=product%2Fcheckout&runWindowMinutes=90", requests.get(0).query);
        JsonNode body = client.getObjectMapper().readTree(requests.get(1).body);
        assertEquals(digest, body.path("expectedSettingsDigest").asText());
    }

    @Test
    void unifiedInputValidationFailsBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () ->
                client.autonomousMissions().getEffectiveSettings(new MissionEffectiveSettingsOptions().runWindowMinutes(20161)));
        assertThrows(IllegalArgumentException.class, () ->
                client.autonomousMissions().start(new MissionStartRequest().goal(" ")));
        assertThrows(IllegalArgumentException.class, () ->
                client.autonomousMissions().start(new MissionStartRequest().goal("x").expectedSettingsDigest("sha256:bad")));
        assertThrows(IllegalArgumentException.class, () -> new MissionStartRequest().budget(-1, 0, 0));
        assertEquals(0, requests.size());
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getRawPath(),
                exchange.getRequestURI().getRawQuery(), exchange.getRequestHeaders().getFirst("Authorization"), body));
        String path = exchange.getRequestURI().getPath();
        String response;
        int status = 200;
        if (path.endsWith("/missions/settings/effective")) {
            response = "{\"namespace\":\"team-a\",\"productId\":\"product/checkout\",\"settingsDigest\":\"sha256:" + "a".repeat(64) + "\",\"count\":1,\"settings\":[{\"key\":\"mission_run_window_minutes\",\"value\":\"90\",\"layer\":\"mission\",\"builtin\":\"480\",\"runtimeApplied\":true}]}";
        } else if (path.equals("/api/v1/missions")) {
            status = 201;
            response = "{\"created\":true,\"mission\":{\"id\":\"m-unified\",\"namespace\":\"team-a\",\"productId\":\"product/checkout\",\"kind\":\"testing\",\"goal\":\"ship checkout\",\"origin\":\"ui\",\"status\":\"queued\",\"chain\":[]}}";
        } else if (path.endsWith("/intents")) {
            status = 202;
            response = "{\"missionId\":\"m-1\",\"status\":\"accepted\"}";
        } else if (path.endsWith("/missions")) {
            response = "{\"missions\":[{\"id\":\"m-1\",\"goal\":\"verify checkout\",\"status\":\"active\"}],\"total\":1}";
        } else if (path.endsWith("/flow")) {
            response = "{\"mission\":{\"id\":\"m-1\",\"goal\":\"verify checkout\",\"status\":\"done\"},\"steps\":[],\"artifacts\":[]}";
        } else {
            response = "{\"id\":\"m-1\",\"goal\":\"verify checkout\",\"status\":\"active\"}";
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
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
