// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V4 pact JSON shape verification.
 *
 * <p>V4-specific invariants from the official spec
 * (https://github.com/pact-foundation/pact-specification/tree/version-4):</p>
 * <ul>
 *   <li>Each interaction carries a {@code type} discriminator
 *       ({@code Synchronous/HTTP} for HTTP).</li>
 *   <li>{@code providerStates} is an array of objects with
 *       {@code name} + optional {@code params}.</li>
 *   <li>{@code matchingRules} is split by category
 *       ({@code body}/{@code header}/{@code query}/{@code path}).</li>
 *   <li>Body category re-roots on {@code $}.</li>
 *   <li>{@code matchers} array per path, with a {@code combine} mode.</li>
 * </ul>
 */
class PactWriterV4FixtureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("V4: category-split rules + providerStates array + interaction type")
    void v4RoundTrip() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", Matchers.integer(100));
        body.put("currency", Matchers.regex("[A-Z]{3}", "USD"));
        body.put("tags", Matchers.minMaxType("tag-1", 1, 5));

        Pact pact = Consumer.named("OrderService")
                .withProvider("PaymentService")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it
                        .given("payment service is up", Map.of("currency", "USD"))
                        .uponReceiving("a V4 charge request")
                        .withRequest("POST", "/charge")
                        .withHeader("X-Trace", Matchers.regex("[0-9a-f]+", "abc123"))
                        .withJsonBody(body)
                        .willRespondWith(201)
                        .withJsonBody(Map.of(
                                "id", Matchers.like("abc"),
                                "items", Matchers.eachLike(Map.of("sku", Matchers.like("X-1"))))))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        assertEquals("4.0", root.path("metadata").path("pactSpecification").path("version").asText());

        JsonNode interaction = root.path("interactions").get(0);
        assertEquals("Synchronous/HTTP", interaction.path("type").asText());

        // providerStates plural array + params
        assertFalse(interaction.has("providerState"));
        JsonNode states = interaction.path("providerStates");
        assertTrue(states.isArray());
        assertEquals("payment service is up", states.get(0).path("name").asText());
        assertEquals("USD", states.get(0).path("params").path("currency").asText());

        // matchingRules: split by category
        JsonNode reqRules = interaction.path("request").path("matchingRules");
        assertTrue(reqRules.has("body"));
        assertTrue(reqRules.has("header"));

        JsonNode bodyRules = reqRules.path("body");
        // V4 re-roots on $ when entering the body category
        assertTrue(bodyRules.has("$.amount"));
        JsonNode amountEntry = bodyRules.path("$.amount");
        assertEquals("AND", amountEntry.path("combine").asText());
        assertTrue(amountEntry.path("matchers").isArray());
        assertEquals("integer", amountEntry.path("matchers").get(0).path("match").asText());

        JsonNode tagsEntry = bodyRules.path("$.tags");
        assertEquals("type", tagsEntry.path("matchers").get(0).path("match").asText());
        assertEquals(1, tagsEntry.path("matchers").get(0).path("min").asInt());
        assertEquals(5, tagsEntry.path("matchers").get(0).path("max").asInt());
    }

    @Test
    @DisplayName("V4: plugin declarations land in metadata.plugins")
    void v4Plugins() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .withPlugin("matt-http")
                .addInteraction(it -> it.uponReceiving("x")
                        .withRequest("GET", "/x").willRespondWith(200))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        JsonNode plugins = root.path("metadata").path("plugins");
        assertTrue(plugins.isArray());
        assertEquals(1, plugins.size());
        assertEquals("matt-http", plugins.get(0).path("name").asText());
    }

    @Test
    @DisplayName("V4: binary body encoded base64")
    void v4BinaryBody() throws Exception {
        byte[] payload = new byte[] {1, 2, 3, 4, 5, (byte) 0xff};
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("binary")
                        .withRequest("POST", "/upload")
                        .withBinaryBody(payload, "application/octet-stream")
                        .willRespondWith(204))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        JsonNode body = root.path("interactions").get(0).path("request").path("body");
        assertEquals("base64", body.path("encoded").asText());
        assertEquals("application/octet-stream", body.path("contentType").asText());
        byte[] decoded = java.util.Base64.getDecoder().decode(body.path("content").asText());
        assertEquals(payload.length, decoded.length);
    }

    @Test
    @DisplayName("V4: nested matcher inside eachLike produces wildcard-path rule")
    void v4NestedMatchers() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it.uponReceiving("nested")
                        .withRequest("GET", "/x")
                        .willRespondWith(200)
                        .withJsonBody(Map.of(
                                "items", Matchers.eachLike(Map.of("name", Matchers.like("foo"))))))
                .build();

        JsonNode rules = MAPPER.readTree(pact.toJson())
                .path("interactions").get(0)
                .path("response").path("matchingRules").path("body");

        // The wildcard-path entry shows the inner matcher type covers all
        // elements at once.
        boolean found = false;
        java.util.Iterator<String> names = rules.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            if (key.contains("[*]") && key.endsWith(".name")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "expected a wildcard-path entry for the nested matcher, got " + rules);
    }
}
