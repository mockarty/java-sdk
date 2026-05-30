// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * In-process wire tests for {@link ContractApi#importPact} — the pact -&gt;
 * Mockarty contract bridge. Spins up an {@link HttpServer}, has the SDK POST
 * against it, and pins the exact wire shape the admin's pactPublish handler
 * binds: {@code {pactContent, version}} to {@code /api/v1/contract/pacts}
 * with the namespace on the query. Also covers offline validation that needs
 * no network round-trip.
 */
class ContractImportPactTest {

    private static final String SAMPLE_PACT_V3 = "{"
            + "\"consumer\":{\"name\":\"WebApp\"},"
            + "\"provider\":{\"name\":\"UserService\"},"
            + "\"interactions\":[{\"description\":\"d\","
            + "\"request\":{\"method\":\"GET\",\"path\":\"/users/1\"},"
            + "\"response\":{\"status\":200,\"body\":{\"id\":1}}}],"
            + "\"metadata\":{\"pactSpecification\":{\"version\":\"3.0.0\"}}}";

    private HttpServer server;
    private MockartyClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> captured = new AtomicReference<>();
    private final AtomicReference<String> capturedQuery = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/contract/pacts", exchange -> {
            capturedQuery.set(exchange.getRequestURI().getQuery());
            captured.set(mapper.readTree(exchange.getRequestBody()));
            byte[] body = ("{\"id\":\"c1\",\"consumer\":{\"name\":\"WebApp\"},"
                    + "\"provider\":{\"name\":\"UserService\"},\"version\":\"3.0.0\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        client = MockartyClient.builder()
                .baseUrl("http://localhost:" + server.getAddress().getPort())
                .apiKey("k")
                .namespace("team-a")
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
    @DisplayName("importPact POSTs {pactContent, version} with version derived from body")
    void postsPactContentEnvelope() throws Exception {
        Map<String, Object> result = client.contracts().importPact(SAMPLE_PACT_V3, null, null);

        JsonNode req = captured.get();
        assertEquals(SAMPLE_PACT_V3, req.get("pactContent").asText(),
                "pactContent must be forwarded verbatim");
        assertEquals("3.0.0", req.get("version").asText(),
                "version derived from metadata.pactSpecification.version");
        assertEquals("namespace=team-a", capturedQuery.get(),
                "client namespace threaded onto the query");

        @SuppressWarnings("unchecked")
        Map<String, Object> consumer = (Map<String, Object>) result.get("consumer");
        assertEquals("WebApp", consumer.get("name"));
    }

    @Test
    @DisplayName("explicit version + namespace override the defaults")
    void explicitOverrides() throws Exception {
        client.contracts().importPact(SAMPLE_PACT_V3, "git-sha-123", "ci-ns");
        assertEquals("git-sha-123", captured.get().get("version").asText());
        assertEquals("namespace=ci-ns", capturedQuery.get());
    }

    @Test
    @DisplayName("importPactFile reads the pact from disk")
    void readsFromDisk() throws Exception {
        Path dir = Files.createTempDirectory("pact-test");
        Path file = dir.resolve("webapp-userservice.json");
        Files.writeString(file, SAMPLE_PACT_V3, StandardCharsets.UTF_8);

        client.contracts().importPactFile(file, "1.0.0", null);
        assertEquals(SAMPLE_PACT_V3, captured.get().get("pactContent").asText());
        assertEquals("1.0.0", captured.get().get("version").asText());
    }

    @Test
    @DisplayName("no spec version omits the version field")
    void noVersionOmitted() throws Exception {
        String pact = "{\"consumer\":{\"name\":\"C\"},\"provider\":{\"name\":\"P\"}}";
        client.contracts().importPact(pact, null, null);
        assertNull(captured.get().get("version"), "version field omitted when unknown");
    }

    @Test
    @DisplayName("missing consumer/provider and malformed JSON fail offline")
    void offlineValidation() {
        assertThrows(MockartyException.class, () ->
                client.contracts().importPact("{\"provider\":{\"name\":\"P\"}}", null, null));
        assertThrows(MockartyException.class, () ->
                client.contracts().importPact("{\"consumer\":{\"name\":\"C\"}}", null, null));
        MockartyException malformed = assertThrows(MockartyException.class, () ->
                client.contracts().importPact("{not json", null, null));
        assertTrue(malformed.getMessage().contains("not valid pact JSON"));
        // The failures happened before any network round-trip.
        assertNull(captured.get());
    }

    @Test
    @DisplayName("importPactFile surfaces a clear error for a missing file")
    void missingFile() {
        MockartyException ex = assertThrows(MockartyException.class, () ->
                client.contracts().importPactFile(Path.of("/no/such/pact.json")));
        assertTrue(ex.getMessage().contains("read"));
    }
}
