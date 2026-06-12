// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mockarty.pact.Matchers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java message-pact content now supports the {@link Matchers} catalogue
 * (previously plain values only): the rendered content carries the example
 * while the matcher metadata is emitted as V4 body {@code matchingRules},
 * matching the Go/Python message DSL and the HTTP body shape the server
 * already normalises.
 */
class MessagePactMatchersTest {

    private static final ObjectMapper M = new ObjectMapper();

    @Test
    @DisplayName("matchers in message content render examples + emit matchingRules (V4)")
    void messageMatchers() throws Exception {
        MessagePact mp = new MessagePact("OrderConsumer", "OrderEvents")
                .given("an order exists")
                .expectsToReceive("an order event")
                .withContent(Map.of(
                        "orderId", Matchers.uuid("550e8400-e29b-41d4-a716-446655440000"),
                        "status", Matchers.regex("open|closed", "open"),
                        "amount", Matchers.decimal(9.99)));

        JsonNode doc = M.readTree(mp.toJson());
        JsonNode ix = doc.path("interactions").get(0);
        JsonNode content = ix.path("contents").path("content");

        // Examples are rendered as concrete values (NOT serialised matcher records).
        assertEquals("550e8400-e29b-41d4-a716-446655440000", content.path("orderId").asText());
        assertEquals("open", content.path("status").asText());
        assertTrue(content.path("amount").isNumber());

        // matchingRules under the body category carry the matcher tags.
        JsonNode bodyRules = ix.path("matchingRules").path("body");
        assertFalse(bodyRules.isMissingNode(), "expected body matchingRules");
        assertEquals("uuid",
                bodyRules.path("$.orderId").path("matchers").get(0).path("match").asText());
        assertEquals("regex",
                bodyRules.path("$.status").path("matchers").get(0).path("match").asText());
        assertEquals("decimal",
                bodyRules.path("$.amount").path("matchers").get(0).path("match").asText());
    }
}
