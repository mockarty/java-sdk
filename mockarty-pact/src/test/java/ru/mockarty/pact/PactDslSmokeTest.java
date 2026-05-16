// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for the surface-level Pact DSL — exercises the builder,
 * the matchers package, and the high-level wiring without diving into
 * JSON shape (that's tested separately in {@link PactWriterV3FixtureTest}
 * and {@link PactWriterV4FixtureTest}).
 */
class PactDslSmokeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("Builder rejects missing required fields")
    void builderEnforcesPreconditions() {
        // Missing provider
        Consumer c1 = Consumer.named("OrderService");
        assertThrows(IllegalStateException.class, c1::build);

        // Missing interactions
        Consumer c2 = Consumer.named("OrderService").withProvider("Pay");
        assertThrows(IllegalStateException.class, c2::build);

        // Missing description on interaction
        Consumer c3 = Consumer.named("OrderService").withProvider("Pay")
                .addInteraction(it -> it.uponReceiving("ok").withRequest("GET", "/p").willRespondWith(200));
        Pact p = c3.build();
        assertEquals(1, p.interactions().size());

        // willRespondWith before withRequest
        assertThrows(IllegalStateException.class, () -> Consumer.named("c").withProvider("p")
                .addInteraction(it -> it.uponReceiving("x").willRespondWith(200))
                .build());
    }

    @Test
    @DisplayName("Plugins require V4; V3 + plugin = hard error")
    void pluginsRequireV4() {
        Consumer c = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V3)
                .withPlugin("matt-http")
                .addInteraction(it -> it.uponReceiving("x").withRequest("GET", "/").willRespondWith(200));
        assertThrows(IllegalStateException.class, c::build);
    }

    @Test
    @DisplayName("Matchers reject malformed input at the factory")
    void matchersValidate() {
        assertThrows(NullPointerException.class, () -> Matchers.like(null));
        assertThrows(IllegalArgumentException.class, () -> Matchers.term("[invalid(", "x"));
        assertThrows(IllegalArgumentException.class, () -> Matchers.minMaxType("x", 10, 5));
        assertThrows(IllegalArgumentException.class, () -> Matchers.arrayContains());
        assertThrows(IllegalArgumentException.class, () -> Matchers.eachKey("x" /* no rules */));
    }

    @Test
    @DisplayName("V4-only matcher in V3 pact → loud error")
    void v4OnlyMatcherInV3Fails() {
        Consumer c = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V3)
                .addInteraction(it -> it.uponReceiving("x")
                        .withRequest("POST", "/p")
                        .withJsonBody(Map.of("v", Matchers.equality("strict")))
                        .willRespondWith(200));
        Pact p = c.build();
        IllegalStateException ex = assertThrows(IllegalStateException.class, p::toJson);
        assertTrue(ex.getMessage().contains("V4-only"),
                "expected V4-only complaint, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Provider-state params under V3 → loud error")
    void providerStateParamsRejectedUnderV3() {
        Consumer c = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V3)
                .addInteraction(it -> it.given("ready", Map.of("count", 7))
                        .uponReceiving("x")
                        .withRequest("GET", "/p")
                        .willRespondWith(200));
        Pact pact = c.build();
        assertThrows(IllegalStateException.class, pact::toJson);
    }

    @Test
    @DisplayName("Top-level JSON shape contains all four required keys")
    void topLevelShape() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", Matchers.like(100));
        Pact pact = Consumer.named("OrderService")
                .withProvider("PaymentService")
                .specVersion(SpecVersion.V4)
                .addInteraction(it -> it
                        .given("payment service is up")
                        .uponReceiving("a charge request")
                        .withRequest("POST", "/charge")
                        .withHeader("Content-Type", "application/json")
                        .withJsonBody(body)
                        .willRespondWith(200)
                        .withJsonBody(Map.of("id", Matchers.like("abc"))))
                .build();

        JsonNode node = MAPPER.readTree(pact.toJson());
        assertEquals("OrderService", node.path("consumer").path("name").asText());
        assertEquals("PaymentService", node.path("provider").path("name").asText());
        assertTrue(node.path("interactions").isArray());
        assertEquals(1, node.path("interactions").size());
        assertEquals("4.0", node.path("metadata").path("pactSpecification").path("version").asText());
    }
}
