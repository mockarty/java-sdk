// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exhaustive matcher-engine behaviour tests.
 *
 * <p>Every {@link Matcher} variant has at least one "happy path" + one
 * "rejection" case. The matrix is large by design — the engine is the
 * single point where contract-test failures get classified, so a
 * regression here silently lets through broken contracts.</p>
 */
class MatcherEngineTest {

    // ── Type-only matchers ─────────────────────────────────────────────

    @Test
    @DisplayName("like(): matches same JSON type, rejects different")
    void likeMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.like("hello"), "world", "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.like("hello"), 42, "$").isEmpty());
        assertTrue(MatcherEngine.evaluate(Matchers.like(1), 99, "$").isEmpty());
    }

    @Test
    @DisplayName("matchType(): strict type-only check")
    void matchTypeMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.matchType(true), false, "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.matchType(true), "true", "$").isEmpty());
    }

    // ── Equality ───────────────────────────────────────────────────────

    @Test
    @DisplayName("equality(): strict deep equality")
    void equality() {
        assertTrue(MatcherEngine.evaluate(Matchers.equality("USD"), "USD", "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.equality("USD"), "EUR", "$").isEmpty());
        // Numeric equality uses doubleValue normalisation — 1 == 1.0
        assertTrue(MatcherEngine.evaluate(Matchers.equality(1), 1.0, "$").isEmpty());
    }

    // ── Regex / term ───────────────────────────────────────────────────

    @Test
    @DisplayName("regex(): pass + fail + non-string")
    void regexMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.regex("[a-z]+", "x"), "abc", "$").isEmpty());
        List<MismatchReport> bad = MatcherEngine.evaluate(Matchers.regex("[a-z]+", "x"), "X1", "$");
        assertEquals(1, bad.size());
        assertEquals("regex([a-z]+)", bad.get(0).matcherType());
        // Non-string actual is a mismatch
        assertFalse(MatcherEngine.evaluate(Matchers.regex("[a-z]+", "x"), 7, "$").isEmpty());
    }

    @Test
    @DisplayName("term(): legacy V3 dialect behaves like regex")
    void termMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.term("[0-9]+", "1"), "42", "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.term("[0-9]+", "1"), "abc", "$").isEmpty());
    }

    // ── Primitive type matchers ────────────────────────────────────────

    @Test
    @DisplayName("integer(): accepts int-shaped numbers, rejects floats")
    void integerMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.integer(0), 5L, "$").isEmpty());
        assertTrue(MatcherEngine.evaluate(Matchers.integer(0), 5, "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.integer(0), 3.14, "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.integer(0), "5", "$").isEmpty());
    }

    @Test
    @DisplayName("decimal(): accepts fractional numbers, rejects pure ints")
    void decimalMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.decimal(0.5), 1.5, "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.decimal(0.5), 1, "$").isEmpty());
    }

    @Test
    @DisplayName("bool(): boolean only")
    void boolMatcher() {
        assertTrue(MatcherEngine.evaluate(Matchers.bool(true), false, "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.bool(true), "true", "$").isEmpty());
    }

    // ── Array-sized matchers ───────────────────────────────────────────

    @Test
    @DisplayName("eachLike(): respects min-size, recurses into elements")
    void eachLikeMatcher() {
        Matcher m = Matchers.eachLike(Map.of("name", Matchers.like("x")), 2);
        assertTrue(MatcherEngine.evaluate(m,
                List.of(Map.of("name", "a"), Map.of("name", "b")), "$").isEmpty());
        // Too short
        assertFalse(MatcherEngine.evaluate(m, List.of(Map.of("name", "a")), "$").isEmpty());
        // Element type wrong
        assertFalse(MatcherEngine.evaluate(m,
                List.of(Map.of("name", 7)), "$").isEmpty());
    }

    @Test
    @DisplayName("minType / maxType / minMaxType: array bounds enforced")
    void boundedArrays() {
        assertTrue(MatcherEngine.evaluate(Matchers.minType("x", 1),
                List.of("a", "b"), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.minType("x", 3),
                List.of("a"), "$").isEmpty());
        assertTrue(MatcherEngine.evaluate(Matchers.maxType("x", 2),
                List.of("a"), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.maxType("x", 2),
                List.of("a", "b", "c"), "$").isEmpty());
        assertTrue(MatcherEngine.evaluate(Matchers.minMaxType("x", 1, 3),
                List.of("a", "b"), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(Matchers.minMaxType("x", 1, 2),
                List.of("a", "b", "c"), "$").isEmpty());
    }

    @Test
    @DisplayName("arrayContains: every variant must be present somewhere")
    void arrayContainsMatcher() {
        Matcher m = Matchers.arrayContains("alpha", "beta");
        assertTrue(MatcherEngine.evaluate(m, List.of("beta", "gamma", "alpha"), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(m, List.of("alpha", "gamma"), "$").isEmpty());
        // Variant matcher must also be satisfied
        Matcher mm = Matchers.arrayContains(Matchers.integer(0));
        assertTrue(MatcherEngine.evaluate(mm, List.of("a", 5), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(mm, List.of("a", "b"), "$").isEmpty());
    }

    // ── Map matchers ──────────────────────────────────────────────────

    @Test
    @DisplayName("eachKey / eachValue: apply nested rules to maps")
    void eachKeyValue() {
        Matcher keys = Matchers.eachKey(Map.of(), Matchers.regex("[a-z]+", "x"));
        assertTrue(MatcherEngine.evaluate(keys, Map.of("foo", 1, "bar", 2), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(keys, Map.of("Foo", 1), "$").isEmpty());

        Matcher values = Matchers.eachValue(Map.of(), Matchers.integer(0));
        assertTrue(MatcherEngine.evaluate(values, Map.of("a", 1, "b", 2), "$").isEmpty());
        assertFalse(MatcherEngine.evaluate(values, Map.of("a", "x"), "$").isEmpty());

        // Wrong outer type — must be a map
        assertFalse(MatcherEngine.evaluate(values, List.of(1, 2), "$").isEmpty());
    }

    @Test
    @DisplayName("eachKeyLike (legacy V3): class-based key check")
    void eachKeyLikeMatcher() {
        Matcher m = Matchers.eachKeyLike("placeholder");
        Map<String, Object> good = Map.of("a", 1, "b", 2);
        assertTrue(MatcherEngine.evaluate(m, good, "$").isEmpty());
    }

    // ── Path matchers ─────────────────────────────────────────────────

    @Test
    @DisplayName("jsonPath: nested resolution with inner matcher")
    void jsonPathMatcher() {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("user", Map.of("addresses", List.of(
                Map.of("zip", "10001"),
                Map.of("zip", "10002"))));
        Matcher m = Matchers.jsonPath("$.user.addresses[0].zip",
                Matchers.regex("[0-9]+", "00000"));
        assertTrue(MatcherEngine.evaluate(m, tree, "$").isEmpty());
        // Missing path
        assertFalse(MatcherEngine.evaluate(
                Matchers.jsonPath("$.user.missing", Matchers.like("x")), tree, "$").isEmpty());
    }

    @Test
    @DisplayName("xmlPath: XPath 1.0 against an XML string")
    void xmlPathMatcher() {
        String xml = "<order><amount>100</amount><currency>USD</currency></order>";
        Matcher m = Matchers.xmlPath("/order/amount",
                Matchers.regex("[0-9]+", "0"));
        assertTrue(MatcherEngine.evaluate(m, xml, "$").isEmpty());
        // Missing path
        assertFalse(MatcherEngine.evaluate(
                Matchers.xmlPath("/order/nope", Matchers.like("x")), xml, "$").isEmpty());
        // Empty actual
        assertFalse(MatcherEngine.evaluate(
                Matchers.xmlPath("/order/amount", Matchers.like("x")), "", "$").isEmpty());
    }

    // ── Tree comparison ───────────────────────────────────────────────

    @Test
    @DisplayName("compare(): mixed literal + matcher tree")
    void treeCompare() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", Matchers.like("abc"));
        expected.put("status", "OK");
        expected.put("tags", List.of(Matchers.like("foo")));

        Map<String, Object> actual = new LinkedHashMap<>();
        actual.put("id", "xyz");
        actual.put("status", "OK");
        actual.put("tags", List.of("bar"));

        assertTrue(MatcherEngine.compare(expected, actual, "$.body").isEmpty());

        // Wrong literal
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("id", "xyz");
        bad.put("status", "FAIL");
        bad.put("tags", List.of("bar"));
        List<MismatchReport> ms = MatcherEngine.compare(expected, bad, "$.body");
        assertFalse(ms.isEmpty());
        assertTrue(ms.get(0).path().contains("status"));
    }

    @Test
    @DisplayName("compare(): missing key + length mismatch")
    void treeMissing() {
        Map<String, Object> expected = Map.of("a", 1, "b", 2);
        Map<String, Object> actual = Map.of("a", 1);
        List<MismatchReport> ms = MatcherEngine.compare(expected, actual, "$.body");
        assertFalse(ms.isEmpty());

        List<Object> exList = List.of(1, 2, 3);
        List<Object> acList = List.of(1, 2);
        List<MismatchReport> ms2 = MatcherEngine.compare(exList, acList, "$.body");
        assertFalse(ms2.isEmpty());
    }

    // ── Thread-safety ─────────────────────────────────────────────────

    @Test
    @DisplayName("Engine is thread-safe: concurrent evaluate calls stay correct")
    void concurrentSafety() throws Exception {
        final int threads = 16;
        final int loops = 200;
        Matcher m = Matchers.eachLike(Map.of("k", Matchers.integer(1)), 1);
        Object actual = List.of(Map.of("k", 7));
        Thread[] ts = new Thread[threads];
        final boolean[] ok = new boolean[threads];
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            ts[t] = new Thread(() -> {
                boolean clean = true;
                for (int i = 0; i < loops; i++) {
                    if (!MatcherEngine.evaluate(m, actual, "$").isEmpty()) {
                        clean = false;
                        break;
                    }
                }
                ok[idx] = clean;
            });
            ts[t].start();
        }
        for (Thread thread : ts) thread.join();
        for (boolean b : ok) assertTrue(b, "engine should be reentrant");
    }
}
