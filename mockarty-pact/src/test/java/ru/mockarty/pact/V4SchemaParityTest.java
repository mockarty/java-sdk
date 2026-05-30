// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Schema-parity guard for V4-format pact JSON.
 *
 * <p>Compares an SDK-emitted pact JSON document against a hand-rolled
 * reference fixture covering the V4-specific invariants. The reference
 * fixture is intentionally inline so changes to the canonical V4 shape
 * (per the pact-foundation spec) surface as test diffs.</p>
 */
class V4SchemaParityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("V4 emitted shape carries pactSpecification.version=4.0 and matchers tree")
    void schemaParity() throws Exception {
        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("amount", Matchers.integer(100));
        reqBody.put("currency", Matchers.equality("USD"));

        Pact pact = Consumer.named("OrderService")
                .withProvider("PaymentService")
                .specVersion(SpecVersion.V4)
                .withPlugin("protobuf")
                .addInteraction(it -> it
                        .uponReceiving("charge")
                        .withRequest("POST", "/charge")
                        .withJsonBody(reqBody)
                        .willRespondWith(200)
                        .withJsonBody(Map.of(
                                "id", Matchers.like("abc"),
                                "status", Matchers.equality("OK"))))
                .build();

        JsonNode root = MAPPER.readTree(pact.toJson());
        // Mandatory V4 fields
        assertEquals("4.0", root.path("metadata").path("pactSpecification").path("version").asText());
        assertTrue(root.path("interactions").isArray());

        JsonNode plugin = root.path("metadata").path("plugins").get(0);
        assertEquals("protobuf", plugin.path("name").asText());
        assertEquals("0.1.0", plugin.path("version").asText(),
                "plugin version must come from the resolved Plugin, not a stub");

        JsonNode interaction = root.path("interactions").get(0);
        assertEquals("Synchronous/HTTP", interaction.path("type").asText());

        // request matching rules categorisation
        JsonNode reqRules = interaction.path("request").path("matchingRules").path("body");
        assertTrue(reqRules.has("$.amount"));
        assertEquals("integer", reqRules.path("$.amount").path("matchers").get(0).path("match").asText());
        assertEquals("equality", reqRules.path("$.currency").path("matchers").get(0).path("match").asText());

        // response matching rules
        JsonNode respRules = interaction.path("response").path("matchingRules").path("body");
        assertEquals("type", respRules.path("$.id").path("matchers").get(0).path("match").asText());
        assertEquals("equality", respRules.path("$.status").path("matchers").get(0).path("match").asText());
    }

    @Test
    @DisplayName("Plugin configuration is serialised under metadata.plugins[].configuration")
    void pluginConfigSerialised() throws Exception {
        Pact pact = Consumer.named("c").withProvider("p")
                .specVersion(SpecVersion.V4)
                .withPlugin("protobuf", Map.of("descriptorPath", "/tmp/svc.desc"))
                .addInteraction(it -> it.uponReceiving("x")
                        .withRequest("GET", "/x").willRespondWith(200))
                .build();
        JsonNode plugin = MAPPER.readTree(pact.toJson())
                .path("metadata").path("plugins").get(0);
        assertEquals("protobuf", plugin.path("name").asText());
        assertEquals("/tmp/svc.desc", plugin.path("configuration").path("descriptorPath").asText());
    }
}
