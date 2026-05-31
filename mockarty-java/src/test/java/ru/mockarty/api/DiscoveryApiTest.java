// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.MockartyRateLimitException;
import ru.mockarty.exception.MockartyValidationException;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryManifestCase;
import ru.mockarty.model.DiscoveryResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-process wire tests for {@link DiscoveryApi#syncDiscovery}. Spins up a
 * {@link HttpServer} on a free port, has the SDK POST against it, and
 * asserts the URL + body shape match the canonical {@code /tcm/discovery}
 * contract and that {@link DiscoveryResult} round-trips.
 */
class DiscoveryApiTest {

    private HttpServer server;
    private MockartyClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("mk_test")
                .namespace("qa")
                .timeout(Duration.ofSeconds(5))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("syncDiscovery POSTs the manifest to /tcm/discovery and parses DiscoveryResult")
    void postsManifestAndParsesResult() throws Exception {
        AtomicReference<JsonNode> captured = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            captured.set(mapper.readTree(exchange.getRequestBody()));

            byte[] body = ("{\"source\":\"junit5:auth-suite\",\"created\":2,"
                    + "\"updated\":1,\"orphaned\":3,\"total\":3}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        DiscoveryManifest manifest = new DiscoveryManifest("junit5:auth-suite")
                .framework("junit5")
                .pruneMissing(true)
                .addCase(new DiscoveryManifestCase("com.example.AuthTest#testLogin", "testLogin")
                        .suite("AuthTest")
                        .sourceRef("AuthTest.java")
                        .labels(List.of("smoke")))
                .addCase(new DiscoveryManifestCase("com.example.AuthTest#testLogout", "testLogout")
                        .suite("AuthTest"));

        DiscoveryResult result = client.discovery().syncDiscovery("qa", manifest);

        // Response round-trip.
        assertNotNull(result);
        assertEquals("junit5:auth-suite", result.getSource());
        assertEquals(2, result.getCreated());
        assertEquals(1, result.getUpdated());
        assertEquals(3, result.getOrphaned());
        assertEquals(3, result.getTotal());

        // Request body shape — exact contract field names.
        JsonNode req = captured.get();
        assertNotNull(req, "server must have received a body");
        assertEquals("junit5:auth-suite", req.get("source").asText());
        assertEquals("junit5", req.get("framework").asText());
        assertTrue(req.get("pruneMissing").asBoolean(), "pruneMissing must serialize when true");
        assertEquals(2, req.get("cases").size());

        JsonNode c0 = req.get("cases").get(0);
        assertEquals("com.example.AuthTest#testLogin", c0.get("fullName").asText());
        assertEquals("testLogin", c0.get("name").asText());
        assertEquals("AuthTest", c0.get("suite").asText());
        assertEquals("AuthTest.java", c0.get("sourceRef").asText());
        assertEquals("smoke", c0.get("labels").get(0).asText());
    }

    @Test
    @DisplayName("pruneMissing=false is dropped from the wire body (matches server omitempty)")
    void pruneMissingFalseOmitted() throws Exception {
        AtomicReference<JsonNode> captured = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            captured.set(mapper.readTree(exchange.getRequestBody()));
            byte[] body = "{\"source\":\"junit5\",\"created\":0,\"updated\":1,\"orphaned\":0,\"total\":1}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        client.discovery().syncDiscovery("qa", new DiscoveryManifest("junit5")
                .addCase(new DiscoveryManifestCase("a#b", "b")));

        JsonNode req = captured.get();
        assertFalse(req.has("pruneMissing"),
                "pruneMissing=false must not be on the wire (additive-only sync)");
    }

    @Test
    @DisplayName("syncDiscovery validates namespace, manifest, source and per-case fullName locally")
    void localValidation() {
        // Missing namespace.
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery("", new DiscoveryManifest("s")));
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery(null, new DiscoveryManifest("s")));
        // Null manifest.
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery("qa", null));
        // Missing source.
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest()));
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest("")));
        // A case without fullName.
        assertThrows(IllegalArgumentException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest("s")
                        .addCase(new DiscoveryManifestCase().name("no-id"))));
    }

    @Test
    @DisplayName("syncDiscovery maps a 400 to MockartyValidationException")
    void invalidManifest400() {
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            byte[] body = "{\"error\":\"source is required\",\"code\":\"validation\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        // Source present locally, so we reach the server which 400s.
        assertThrows(MockartyValidationException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest("s")
                        .addCase(new DiscoveryManifestCase("a#b", "b"))));
    }

    @Test
    @DisplayName("syncDiscovery maps a 429 (server busy) to MockartyRateLimitException")
    void serverBusy429() {
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            byte[] body = "{\"error\":\"too many concurrent discovery syncs\",\"code\":\"rate_limit\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Retry-After", "1");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(429, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        assertThrows(MockartyRateLimitException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest("s")
                        .addCase(new DiscoveryManifestCase("a#b", "b"))));
    }

    @Test
    @DisplayName("syncDiscovery surfaces a 5xx as MockartyException")
    void serverError() {
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            byte[] body = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        assertThrows(MockartyException.class, () ->
                client.discovery().syncDiscovery("qa", new DiscoveryManifest("s")
                        .addCase(new DiscoveryManifestCase("a#b", "b"))));
    }

    @Test
    @DisplayName("an empty-cases manifest is accepted locally and posted (additive no-op)")
    void emptyCasesAllowed() throws Exception {
        AtomicReference<JsonNode> captured = new AtomicReference<>();
        server.createContext("/api/v1/namespaces/qa/tcm/discovery", exchange -> {
            captured.set(mapper.readTree(exchange.getRequestBody()));
            byte[] body = "{\"source\":\"s\",\"created\":0,\"updated\":0,\"orphaned\":0,\"total\":0}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        DiscoveryResult res = client.discovery().syncDiscovery("qa", new DiscoveryManifest("s"));
        assertEquals(0, res.getTotal());
        assertEquals("s", captured.get().get("source").asText());
    }
}
