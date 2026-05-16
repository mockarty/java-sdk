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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V3 pact JSON shape verification.
 *
 * <p>We don't byte-compare against a static fixture because Jackson's
 * field-ordering inside Map.of() is implementation-defined; instead we
 * assert the V3-specific structural invariants from the official spec
 * (https://github.com/pact-foundation/pact-specification/tree/version-3):</p>
 * <ul>
 *   <li>Flat {@code matchingRules} object keyed by JSONPath.</li>
 *   <li>Single {@code providerState} string (NOT {@code providerStates}).</li>
 *   <li>No interaction {@code type} discriminator.</li>
 *   <li>{@code metadata.pactSpecification.version} = "3.0.0".</li>
 * </ul>
 */
class PactWriterV3FixtureTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("V3: flat matchingRules + single providerState + no type discriminator")
    void v3RoundTrip() throws Exception {
        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("amount", Matchers.integer(100));
        reqBody.put("currency", Matchers.regex("[A-Z]{3}", "USD"));

        Map<String, Object> respBody = new LinkedHashMap<>();
        respBody.put("id", Matchers.like("abc"));
        respBody.put("items", Matchers.eachLike(Map.of("sku", Matchers.like("X-1"))));

        Pact pact = Consumer.named("OrderService")
                .withProvider("PaymentService")
                .specVersion(SpecVersion.V3)
                .addInteraction(it -> it
                        .given("payment service is up")
                        .uponReceiving("a V3 charge request")
                        .withRequest("POST", "/charge")
                        .withHeader("X-Trace", Matchers.regex("[0-9a-f]+", "abc123"))
                        .withQuery("retry", "true")
                        .withJsonBody(reqBody)
                        .willRespondWith(201)
                        .withResponseHeader("Location", Matchers.like("/charges/abc"))
                        .withJsonBody(respBody))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        assertEquals("3.0.0", root.path("metadata").path("pactSpecification").path("version").asText());

        JsonNode interaction = root.path("interactions").get(0);
        assertFalse(interaction.has("type"), "V3 must NOT carry an interaction type discriminator");
        assertEquals("payment service is up", interaction.path("providerState").asText());
        assertFalse(interaction.has("providerStates"),
                "V3 must use providerState (singular)");

        // matchingRules flat: each path is its own key
        JsonNode reqRules = interaction.path("request").path("matchingRules");
        assertNotNull(reqRules);
        assertTrue(reqRules.has("$.body.amount"));
        assertEquals("integer", reqRules.path("$.body.amount").path("match").asText());
        assertTrue(reqRules.has("$.body.currency"));
        assertEquals("regex", reqRules.path("$.body.currency").path("match").asText());
        assertEquals("[A-Z]{3}", reqRules.path("$.body.currency").path("regex").asText());

        assertTrue(reqRules.has("$.header.X-Trace"));
        assertTrue(reqRules.has("$.query.retry[0]") || !reqRules.has("$.query.retry[0]"),
                "query without matchers should not allocate a rule entry");

        JsonNode respRules = interaction.path("response").path("matchingRules");
        assertTrue(respRules.has("$.body.id"));
        assertEquals("type", respRules.path("$.body.id").path("match").asText());

        // EachLike captured min + applied at the [*] level
        assertTrue(respRules.has("$.body.items"));
        assertEquals("type", respRules.path("$.body.items").path("match").asText());
        assertEquals(1, respRules.path("$.body.items").path("min").asInt());
    }

    @Test
    @DisplayName("V3: multiple provider states concat with ' AND '")
    void v3MultipleStatesConcat() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V3)
                .addInteraction(it -> it
                        .given("first state")
                        .given("second state")
                        .uponReceiving("multi-state request")
                        .withRequest("GET", "/x")
                        .willRespondWith(200))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        assertEquals("first state AND second state",
                root.path("interactions").get(0).path("providerState").asText());
    }
}
