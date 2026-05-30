// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockServerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    @Test
    @DisplayName("Full consumer flow: client → mock server → response body")
    void fullFlow(@TempDir Path outDir) throws Exception {
        Pact pact = Consumer.named("OrderService")
                .withProvider("PaymentService")
                .specVersion(SpecVersion.V4)
                .outputDir(outDir)
                .addInteraction(it -> it
                        .given("ready")
                        .uponReceiving("charge")
                        .withRequest("POST", "/charge")
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(Map.of("amount", Matchers.integer(100)))
                        .willRespondWith(200)
                        .withJsonBody(Map.of("id", Matchers.like("ch_xyz"))))
                .build();

        Path written;
        try (MockServer mock = MockServer.start(pact)) {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/charge"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":100}"))
                            .timeout(Duration.ofSeconds(2))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(200, resp.statusCode());
            JsonNode body = MAPPER.readTree(resp.body());
            assertEquals("ch_xyz", body.path("id").asText());
            mock.verify();
            written = outDir.resolve("orderservice-paymentservice.json");
        }
        assertTrue(Files.exists(written), "pact JSON should be written on close: " + written);
        // The written file has the schema we'd expect — sanity check
        JsonNode root = MAPPER.readTree(Files.readString(written));
        assertEquals("OrderService", root.path("consumer").path("name").asText());
    }

    @Test
    @DisplayName("verify() fails when a declared interaction goes uncalled")
    void verifyDetectsUncalled() {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("never called")
                        .withRequest("GET", "/x").willRespondWith(200))
                .build();
        try (MockServer mock = MockServer.start(pact, /*writeOnClose=*/false)) {
            AssertionError err = assertThrows(AssertionError.class, mock::verify);
            assertTrue(err.getMessage().contains("Uncalled"),
                    "expected uncalled-interaction complaint, got: " + err.getMessage());
        }
    }

    @Test
    @DisplayName("verify() fails when the consumer hits an unexpected route")
    void verifyDetectsUnexpected() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("only one")
                        .withRequest("GET", "/known").willRespondWith(200))
                .build();
        try (MockServer mock = MockServer.start(pact, /*writeOnClose=*/false)) {
            // Hit the declared route to satisfy "uncalled" check
            HTTP.send(HttpRequest.newBuilder(URI.create(mock.uri() + "/known")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            // Now an unexpected route
            HTTP.send(HttpRequest.newBuilder(URI.create(mock.uri() + "/unknown")).GET().build(),
                    HttpResponse.BodyHandlers.discarding());

            AssertionError err = assertThrows(AssertionError.class, mock::verify);
            assertTrue(err.getMessage().contains("Unexpected"),
                    "expected unexpected-request complaint, got: " + err.getMessage());
        }
    }

    @Test
    @DisplayName("Header matchers: regex on a request header")
    void headerRegexMatcher() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("with-trace")
                        .withRequest("GET", "/trace")
                        .withHeader("X-Trace", Matchers.regex("[0-9a-f]+", "abc"))
                        .willRespondWith(200))
                .build();
        try (MockServer mock = MockServer.start(pact, false)) {
            HttpResponse<String> ok = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/trace"))
                            .header("X-Trace", "deadbeef").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ok.statusCode());

            HttpResponse<String> bad = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/trace"))
                            .header("X-Trace", "NOT-HEX").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertNotEquals(200, bad.statusCode());
        }
    }

    @Test
    @DisplayName("Parallel mock servers do not share ports (no leakage)")
    void parallelIsolation() throws IOException {
        Pact p = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("x")
                        .withRequest("GET", "/").willRespondWith(204))
                .build();
        try (MockServer a = MockServer.start(p, false);
             MockServer b = MockServer.start(p, false)) {
            assertNotEquals(a.uri().getPort(), b.uri().getPort(),
                    "two ephemeral servers must bind to different ports");
        }
    }

    @Test
    @DisplayName("No port leak: 25 server start/stop cycles")
    void noPortLeak() throws IOException {
        // Restart many times in the same JVM to flush any port-leak bug
        // that would surface as 'Address already in use' or as too-many-
        // open-files (the executor isn't shutdown cleanly, etc.). The
        // exact threshold is intentionally low to keep CI fast; bumping
        // to 1000 caught a real leak during development.
        Pact p = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("x")
                        .withRequest("GET", "/").willRespondWith(204))
                .build();
        for (int i = 0; i < 25; i++) {
            try (MockServer ms = MockServer.start(p, false)) {
                assertTrue(ms.uri().getPort() > 0);
            }
        }
    }

    @Test
    @DisplayName("Unicode descriptions survive a round-trip")
    void unicodeDescription() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("Подтверждение списания 💳")
                        .withRequest("GET", "/x").willRespondWith(200))
                .build();
        String json = pact.toJson();
        JsonNode root = MAPPER.readTree(json.getBytes(StandardCharsets.UTF_8));
        assertEquals("Подтверждение списания 💳",
                root.path("interactions").get(0).path("description").asText());
    }
}
