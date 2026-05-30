// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Strict-mode contract behaviour of {@link MockServer}: every declared
 * matcher must accept the inbound payload, otherwise the server replies
 * 422 + the mock fails verification.
 */
class MockServerStrictTest {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).build();

    @Test
    @DisplayName("Strict mode rejects body that violates a regex matcher")
    void rejectsBadJsonRegex() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("regex")
                        .withRequest("POST", "/r")
                        .withJsonBody(Map.of("currency", Matchers.regex("[A-Z]{3}", "USD")))
                        .willRespondWith(200))
                .build();

        try (MockServer mock = MockServer.start(pact, false)) {
            HttpResponse<String> bad = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/r"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"currency\":\"usd\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(422, bad.statusCode());
            assertTrue(bad.body().contains("regex"));
            assertFalse(mock.mismatches().isEmpty());

            AssertionError err = assertThrows(AssertionError.class, mock::verify);
            assertTrue(err.getMessage().contains("Mismatches"));
        }
    }

    @Test
    @DisplayName("Strict mode accepts a body that satisfies all matchers")
    void acceptsValidBody() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("ok")
                        .withRequest("POST", "/r")
                        .withJsonBody(Map.of(
                                "amount", Matchers.integer(0),
                                "currency", Matchers.regex("[A-Z]{3}", "USD")))
                        .willRespondWith(204))
                .build();

        try (MockServer mock = MockServer.start(pact, false)) {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/r"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"amount\":100,\"currency\":\"EUR\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(204, resp.statusCode());
            assertTrue(mock.mismatches().isEmpty());
            mock.verify();
        }
    }

    @Test
    @DisplayName("Non-strict (legacy) mode lets a non-matching body through")
    void nonStrictPasses() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("legacy")
                        .withRequest("POST", "/r")
                        .withJsonBody(Map.of("currency", Matchers.regex("[A-Z]{3}", "USD")))
                        .willRespondWith(200))
                .build();
        try (MockServer mock = MockServer.start(pact, false, /*strict=*/false)) {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/r"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"currency\":\"usd\"}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            // Legacy: 200 OK because the body wasn't validated.
            assertEquals(200, resp.statusCode());
            mock.verify();
        }
    }

    @Test
    @DisplayName("eachLike size violation is detected at strict layer")
    void eachLikeBoundary() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("array")
                        .withRequest("POST", "/a")
                        .withJsonBody(Map.of("items", Matchers.eachLike(Map.of("name", "x"), 2)))
                        .willRespondWith(200))
                .build();
        try (MockServer mock = MockServer.start(pact, false)) {
            HttpResponse<String> resp = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/a"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"items\":[{\"name\":\"a\"}]}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(422, resp.statusCode());
        }
    }

    @Test
    @DisplayName("Plugin-owned content type bypasses JSON path and goes to plugin")
    void pluginRouting() throws Exception {
        // Build a gRPC-shaped expectation body — empty payload is enough,
        // the plugin compares bytes. Wrap actual payload in a valid gRPC
        // frame so the plugin's framing check passes.
        byte[] payload = new byte[] {0x08, 0x01};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(payload.length);
        out.write(payload);
        byte[] framed = out.toByteArray();

        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .withPlugin("grpc")
                .addInteraction(it -> it.uponReceiving("grpc-call")
                        .withRequest("POST", "/g")
                        .withBinaryBody(framed, "application/grpc")
                        .willRespondWith(200))
                .build();

        try (MockServer mock = MockServer.start(pact, false)) {
            HttpResponse<String> ok = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/g"))
                            .header("Content-Type", "application/grpc")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(framed))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, ok.statusCode());

            // Send a different framed payload — plugin must reject.
            byte[] differentPayload = new byte[] {0x08, 0x42};
            ByteArrayOutputStream out2 = new ByteArrayOutputStream();
            out2.write(0);
            out2.write(0);
            out2.write(0);
            out2.write(0);
            out2.write(differentPayload.length);
            out2.write(differentPayload);
            HttpResponse<String> bad = HTTP.send(
                    HttpRequest.newBuilder(URI.create(mock.uri() + "/g"))
                            .header("Content-Type", "application/grpc")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(out2.toByteArray()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(422, bad.statusCode());
        }
    }
}
