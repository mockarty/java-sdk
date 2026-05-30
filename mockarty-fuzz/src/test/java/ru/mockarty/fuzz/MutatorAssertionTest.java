// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutatorAssertionTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static Target.Builder baseBuilder() {
        return Target.named("muta")
                .httpEndpoint("POST", "/x")
                .seeds(Seed.of("s", "{}"));
    }

    @Test
    void everyBuiltInMutatorSerialises() throws Exception {
        Mutator[] all = {Mutator.JSON, Mutator.XML, Mutator.BYTES, Mutator.STRING,
                Mutator.URL, Mutator.HEADER, Mutator.GRPC, Mutator.GRAPHQL};
        String[] expectedNames = {"json", "xml", "bytes", "string", "url", "header", "grpc", "graphql"};
        for (int i = 0; i < all.length; i++) {
            Target t = baseBuilder().mutator(all[i]).build();
            JsonNode opts = MAPPER.readTree(t.toJson()).get("options");
            assertEquals(expectedNames[i], opts.get("mutationTypes").get(0).asText());
            assertEquals(expectedNames[i], opts.get("mutators").get(0).get("name").asText());
            // Built-in mutators have empty config → no `config` key.
            assertFalse(opts.get("mutators").get(0).has("config"),
                    "built-in mutator " + expectedNames[i] + " should not emit config");
        }
    }

    @Test
    void customMutatorCarriesConfigBlob() throws Exception {
        Mutator m = Mutator.custom("my-js-mutator", Map.of("script", "mutate.js", "depth", 3));
        Target t = baseBuilder().mutator(m).build();
        JsonNode opts = MAPPER.readTree(t.toJson()).get("options");
        JsonNode entry = opts.get("mutators").get(0);
        assertEquals("my-js-mutator", entry.get("name").asText());
        assertEquals("mutate.js", entry.get("config").get("script").asText());
        assertEquals(3, entry.get("config").get("depth").asInt());
    }

    @Test
    void customMutatorBlankNameRejected() {
        assertThrows(IllegalArgumentException.class, () -> Mutator.custom(""));
        assertThrows(IllegalArgumentException.class, () -> Mutator.custom("   "));
        assertThrows(NullPointerException.class, () -> Mutator.custom(null));
    }

    @Test
    void mutatorEqualityAndHashCode() {
        Mutator a = Mutator.custom("x", Map.of("k", 1));
        Mutator b = Mutator.custom("x", Map.of("k", 1));
        Mutator c = Mutator.custom("x", Map.of("k", 2));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void statusAssertionRangeValidated() {
        assertThrows(IllegalArgumentException.class, () -> Assertion.statusInRange(99, 200));
        assertThrows(IllegalArgumentException.class, () -> Assertion.statusInRange(300, 200));
        assertThrows(IllegalArgumentException.class, () -> Assertion.statusInRange(200, 600));
    }

    @Test
    void noCrashAssertionIsSingleton() {
        // No-args records ought to share the singleton — saves allocations
        // in the hot path of dozens of targets per CI run.
        assertSame(Assertion.noCrash(), Assertion.noCrash());
    }

    @Test
    void responseTimeUnderRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class,
                () -> Assertion.responseTimeUnder(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> Assertion.responseTimeUnder(Duration.ofSeconds(-1)));
    }

    @Test
    void noErrorInBodyRejectsBlankTokens() {
        assertThrows(IllegalArgumentException.class,
                () -> Assertion.noErrorInBody("ok", ""));
        assertThrows(IllegalArgumentException.class,
                () -> Assertion.noErrorInBody("ok", (String) null));
    }

    @Test
    void noErrorInBodyAllowsEmptyVarargs() throws Exception {
        // Empty token list = "use engine defaults". Transpiles to an
        // empty array — the engine substitutes its built-in corpus.
        Assertion a = Assertion.noErrorInBody();
        Target t = baseBuilder().mutator(Mutator.JSON).assertion(a).build();
        JsonNode opt = MAPPER.readTree(t.toJson()).get("options")
                .get("assertions").get(0);
        assertEquals("no_error_in_body", opt.get("type").asText());
        assertEquals(0, opt.get("errorTokens").size());
    }

    @Test
    void everyAssertionVariantSerialises() throws Exception {
        Target t = baseBuilder()
                .mutator(Mutator.JSON)
                .assertion(Assertion.statusInRange(200, 299))
                .assertion(Assertion.noCrash())
                .assertion(Assertion.responseTimeUnder(Duration.ofMillis(750)))
                .assertion(Assertion.noErrorInBody("stack trace", "SQLSTATE"))
                .build();
        JsonNode arr = MAPPER.readTree(t.toJson()).get("options").get("assertions");
        assertEquals(4, arr.size());
        assertEquals("status_in_range", arr.get(0).get("type").asText());
        assertEquals("no_crash", arr.get(1).get("type").asText());
        assertEquals("response_time_under", arr.get(2).get("type").asText());
        assertEquals(750L, arr.get(2).get("limitMs").asLong());
        assertEquals("no_error_in_body", arr.get(3).get("type").asText());
        assertEquals("stack trace", arr.get(3).get("errorTokens").get(0).asText());
    }

    @Test
    void noErrorInBodyDefensiveCopy() {
        String[] tokens = {"a", "b"};
        Assertion.NoErrorInBody a = Assertion.noErrorInBody(tokens);
        tokens[0] = "MUTATED";
        // Internal array is a clone — caller mutation doesn't leak in.
        assertArrayEquals(new String[]{"a", "b"}, a.errorTokens());
        // Getter also clones on the way out.
        String[] returned = a.errorTokens();
        returned[0] = "ALSO_MUTATED";
        assertArrayEquals(new String[]{"a", "b"}, a.errorTokens());
    }

    @Test
    void multipleMutatorsAllSerialise() throws Exception {
        Target t = baseBuilder()
                .mutators(Mutator.JSON, Mutator.STRING, Mutator.HEADER)
                .build();
        JsonNode mt = MAPPER.readTree(t.toJson()).get("options").get("mutationTypes");
        assertEquals(3, mt.size());
        assertTrue(t.mutators().contains(Mutator.JSON));
        assertTrue(t.mutators().contains(Mutator.STRING));
        assertTrue(t.mutators().contains(Mutator.HEADER));
    }
}
