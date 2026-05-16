// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetBuilderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void requiredFieldsRejectMissingEndpoint() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Target.named("no-endpoint")
                        .seeds(Seed.of("s", "{}"))
                        .mutator(Mutator.JSON)
                        .build());
        assertTrue(ex.getMessage().contains("no endpoint"));
    }

    @Test
    void requiredFieldsRejectEmptySeedCorpus() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Target.named("no-seeds")
                        .httpEndpoint("POST", "/x")
                        .mutator(Mutator.JSON)
                        .build());
        assertTrue(ex.getMessage().contains("no seeds"));
    }

    @Test
    void payloadCategoryAloneAllowsEmptySeeds() {
        // Engine's payload-category mode generates its own corpus, so an
        // empty seeds list IS legal when at least one category is set.
        Target t = Target.named("cat-only")
                .httpEndpoint("POST", "https://api.example.com", "/x")
                .payloadCategory("sqli")
                .build();
        assertEquals("cat-only", t.name());
        assertTrue(t.seeds().isEmpty());
    }

    @Test
    void requiredFieldsRejectNoMutatorAndNoCategory() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Target.named("no-mutator")
                        .httpEndpoint("POST", "/x")
                        .seeds(Seed.of("s", "{}"))
                        .build());
        assertTrue(ex.getMessage().contains("no mutators"));
    }

    @Test
    void blankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> Target.named(""));
        assertThrows(IllegalArgumentException.class, () -> Target.named("   "));
        assertThrows(IllegalArgumentException.class, () -> Target.named(null));
    }

    @Test
    void schemaRoundTripMatchesCanonicalFields() throws Exception {
        Target target = Target.named("login-flow")
                .description("Stress-test login endpoint")
                .namespace("default")
                .httpEndpoint("POST", "https://api.example.com", "/api/v1/login")
                .seeds(
                        Seed.of("valid", "{\"username\":\"admin\",\"password\":\"secret\"}"),
                        Seed.of("missing-pw", "{\"username\":\"admin\"}"))
                .mutator(Mutator.JSON)
                .duration(Duration.ofMinutes(5))
                .stopOnFinding(true)
                .reporter(Reporter.ALLURE)
                .assertion(Assertion.statusInRange(200, 299))
                .build();

        JsonNode node = MAPPER.readTree(target.toJson());

        assertEquals("login-flow", node.get("name").asText());
        assertEquals("Stress-test login endpoint", node.get("description").asText());
        assertEquals("default", node.get("namespace").asText());
        // Parity with FuzzConfig.SourceType — SDK-emitted = "manual".
        assertEquals("manual", node.get("sourceType").asText());
        assertEquals("all", node.get("strategy").asText());
        assertEquals("http", node.get("protocol").asText());
        assertEquals("POST", node.get("method").asText());
        assertEquals("https://api.example.com", node.get("targetBaseUrl").asText());
        assertEquals("/api/v1/login", node.get("path").asText());

        JsonNode seeds = node.get("seedRequests");
        assertTrue(seeds.isArray());
        assertEquals(2, seeds.size());
        assertEquals("valid", seeds.get(0).get("id").asText());
        // Text seeds carry `body`, not `bytesBase64`.
        assertTrue(seeds.get(0).has("body"));
        assertFalse(seeds.get(0).has("bytesBase64"));

        JsonNode opts = node.get("options");
        assertEquals("5m", opts.get("maxDuration").asText());
        assertTrue(opts.get("stopOnCritical").asBoolean());
        assertEquals("allure", opts.get("reporters").get(0).asText());
        assertEquals("json", opts.get("mutationTypes").get(0).asText());
        assertEquals("status_in_range", opts.get("assertions").get(0).get("type").asText());
        assertEquals(200, opts.get("assertions").get(0).get("min").asInt());
        assertEquals(299, opts.get("assertions").get(0).get("max").asInt());
    }

    @Test
    void binarySeedEmitsBase64() throws Exception {
        Target target = Target.named("bin")
                .httpEndpoint("PUT", "/upload")
                .seeds(Seed.bytes("blob", new byte[]{0x00, 0x01, (byte) 0xff}))
                .mutator(Mutator.BYTES)
                .build();
        JsonNode node = MAPPER.readTree(target.toJson());
        JsonNode seed = node.get("seedRequests").get(0);
        assertFalse(seed.has("body"));
        assertTrue(seed.has("bytesBase64"));
        // base64("\x00\x01\xff") = "AAH/"
        assertEquals("AAH/", seed.get("bytesBase64").asText());
    }

    @Test
    void grpcEndpointSerialisesProtocolFields() throws Exception {
        Target target = Target.named("grpc-fuzz")
                .grpcEndpoint("localhost:9090", "user.UserService", "GetUser")
                .seeds(Seed.of("default", "{}"))
                .mutator(Mutator.GRPC)
                .build();
        JsonNode node = MAPPER.readTree(target.toJson());
        assertEquals("grpc", node.get("protocol").asText());
        assertEquals("localhost:9090", node.get("targetBaseUrl").asText());
        assertEquals("user.UserService", node.get("grpcService").asText());
        assertEquals("GetUser", node.get("grpcMethod").asText());
    }

    @Test
    void graphQLEndpointSerialisesProtocolFields() throws Exception {
        Target target = Target.named("gql-fuzz")
                .graphQLEndpoint("https://api.example.com", "/api/graphql")
                .seeds(Seed.of("default", "query { me { id } }"))
                .mutator(Mutator.GRAPHQL)
                .build();
        JsonNode node = MAPPER.readTree(target.toJson());
        assertEquals("graphql", node.get("protocol").asText());
        assertEquals("POST", node.get("method").asText());
        assertEquals("/api/graphql", node.get("graphqlPath").asText());
        assertEquals("/api/graphql", node.get("path").asText());
    }

    @Test
    void fluentChainIsOrderIndependent() throws Exception {
        // Builder methods can be called in any order and the resulting
        // JSON shape is identical — guards against accidental order
        // dependencies (e.g. mutator() resetting seeds()).
        Target a = Target.named("x")
                .httpEndpoint("GET", "/y")
                .seed(Seed.of("s1", "1"))
                .mutator(Mutator.STRING)
                .duration(Duration.ofSeconds(30))
                .build();
        Target b = Target.named("x")
                .duration(Duration.ofSeconds(30))
                .mutator(Mutator.STRING)
                .seed(Seed.of("s1", "1"))
                .httpEndpoint("GET", "/y")
                .build();
        assertEquals(a.toJson(), b.toJson());
    }

    @Test
    void headersAreSerialisedAsMap() throws Exception {
        Target t = Target.named("h")
                .httpEndpoint("GET", "/x")
                .header("Authorization", "Bearer abc")
                .header("X-Trace-Id", "t-1")
                .seeds(Seed.of("s", ""))
                .mutator(Mutator.HEADER)
                .build();
        JsonNode node = MAPPER.readTree(t.toJson());
        assertEquals("Bearer abc", node.get("headers").get("Authorization").asText());
        assertEquals("t-1", node.get("headers").get("X-Trace-Id").asText());
    }

    @Test
    void writeToProducesParseableJsonOnDisk(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        Target t = Target.named("disk")
                .httpEndpoint("POST", "/x")
                .seeds(Seed.of("s", "{}"))
                .mutator(Mutator.JSON)
                .build();
        java.nio.file.Path out = dir.resolve("nested").resolve("target.json");
        t.writeTo(out);
        assertTrue(java.nio.file.Files.exists(out));
        JsonNode node = MAPPER.readTree(out.toFile());
        assertEquals("disk", node.get("name").asText());
    }

    @Test
    void prettyJsonIsParseableAndDifferentFromCompact() throws Exception {
        Target t = Target.named("p")
                .httpEndpoint("POST", "/x")
                .seeds(Seed.of("s", "{}"))
                .mutator(Mutator.JSON)
                .build();
        String compact = Transpiler.toJson(t);
        String pretty = Transpiler.toPrettyJson(t);
        assertNotNull(MAPPER.readTree(compact));
        assertNotNull(MAPPER.readTree(pretty));
        // Pretty form has newlines; compact does not.
        assertTrue(pretty.contains("\n"));
        assertFalse(compact.contains("\n"));
    }

    @Test
    void durationFormatRendersGoCompat() {
        assertEquals("5m", Transpiler.formatDuration(Duration.ofMinutes(5)));
        assertEquals("30s", Transpiler.formatDuration(Duration.ofSeconds(30)));
        assertEquals("1h30m", Transpiler.formatDuration(Duration.ofMinutes(90)));
        assertEquals("1h30m45s", Transpiler.formatDuration(Duration.ofMinutes(90).plusSeconds(45)));
        // Zero seconds (sub-second only) still rounds to a "0s" string —
        // not "" which would break the engine's Go-side time.ParseDuration.
        assertEquals("0s", Transpiler.formatDuration(Duration.ofMillis(500)));
    }
}
