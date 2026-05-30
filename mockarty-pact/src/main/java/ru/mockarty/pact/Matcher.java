// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.List;

/**
 * Sealed hierarchy of Pact matchers.
 *
 * <p>Each variant is a {@code record} carrying both the example value
 * (used to populate the actual body that the mock server returns) and the
 * matching-rule metadata (serialised separately into
 * {@code matchingRules}).</p>
 *
 * <p>Use the {@link Matchers} static factory class for ergonomic
 * construction — never instantiate records directly outside the package.</p>
 *
 * <p>V3-only and V4-only variants are marked explicitly; the
 * {@link PactWriter} fails loud if a V4-only matcher leaks into a V3 pact
 * (per Owner Q10 from SDK_FRAMEWORK_PLAN: no silent downgrades).</p>
 */
public sealed interface Matcher
        permits
        Matcher.Like,
        Matcher.Term,
        Matcher.EachLike,
        Matcher.EachKeyLike,
        Matcher.Regex,
        Matcher.Integer,
        Matcher.Decimal,
        Matcher.Bool,
        Matcher.MatchType,
        Matcher.MinType,
        Matcher.MaxType,
        Matcher.MinMaxType,
        Matcher.ArrayContains,
        Matcher.Equality,
        Matcher.EachKey,
        Matcher.EachValue,
        Matcher.JsonPath,
        Matcher.XmlPath {

    /** The example value embedded into the request/response body. */
    Object example();

    /** Whether this matcher uses constructs that only Pact V4 understands.
     * The writer fails when a V4-only matcher appears under V3. */
    boolean v4Only();

    // ── V3 + V4 matchers ────────────────────────────────────────────

    /** Like-matcher: same JSON type as the example. */
    record Like(Object example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    /** Regex / term matcher: value must match the regex. */
    record Term(String regex, Object example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    /** Each-like: array where every element matches the example shape. */
    record EachLike(Object example, int min) implements Matcher {
        public EachLike(Object example) { this(example, 1); }
        public boolean v4Only() { return false; }
    }

    /** Each-key-like (V3 dialect): legacy map-key matcher kept for
     * cross-tool compatibility. V4 prefers {@link EachKey}. */
    record EachKeyLike(Object example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    /** Plain regex matcher with no example tied to it (V3 + V4).
     * The record component is named {@code exampleValue} so the
     * inherited {@code example()} accessor (declared on this interface)
     * doesn't clash with the auto-generated record accessor. */
    record Regex(String pattern, String exampleValue) implements Matcher {
        public Object example() { return exampleValue; }
        public boolean v4Only() { return false; }
    }

    /** Integer type matcher. */
    record Integer(Number example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    /** Decimal type matcher. */
    record Decimal(Number example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    /** Boolean type matcher. */
    record Bool(Boolean example) implements Matcher {
        public boolean v4Only() { return false; }
    }

    // ── V4-only matchers ─────────────────────────────────────────────

    /** V4 matchType — explicit type-checking variant. */
    record MatchType(Object example) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 minType — array with at least N elements. */
    record MinType(Object example, int min) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 maxType — array with at most N elements. */
    record MaxType(Object example, int max) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 minMaxType — bounded array. */
    record MinMaxType(Object example, int min, int max) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 arrayContains — assertion that all listed variants exist
     * somewhere in the array. */
    record ArrayContains(List<Object> variants) implements Matcher {
        @Override public Object example() { return variants; }
        @Override public boolean v4Only() { return true; }
    }

    /** V4 equality — strict equality (escape hatch for situations where
     * Pact's default type-matching would be too loose). */
    record Equality(Object example) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 eachKey — apply matcher logic to every map key. */
    record EachKey(Object example, List<Matcher> rules) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /** V4 eachValue — apply matcher logic to every map value. */
    record EachValue(Object example, List<Matcher> rules) implements Matcher {
        public boolean v4Only() { return true; }
    }

    /**
     * V4 jsonPath — applies a nested {@link Matcher} to the value resolved
     * by the given JSONPath-style expression. Path syntax is a deliberately
     * small subset (dot-keys, {@code [index]}, root {@code $}) — full
     * JSONPath dialects vary across implementations and the SDK ships its
     * own evaluator (no third-party JSONPath dep).
     */
    record JsonPath(String path, Matcher inner) implements Matcher {
        @Override public Object example() { return inner.example(); }
        @Override public boolean v4Only() { return true; }
    }

    /**
     * V4 xmlPath — applies a nested {@link Matcher} to the value resolved
     * by an XPath-1.0 expression on the request/response body. Internal
     * evaluation uses {@code javax.xml.xpath} (JDK builtin, no extra dep)
     * and is therefore safe for the SDK's air-gapped story.
     */
    record XmlPath(String path, Matcher inner) implements Matcher {
        @Override public Object example() { return inner.example(); }
        @Override public boolean v4Only() { return true; }
    }
}
