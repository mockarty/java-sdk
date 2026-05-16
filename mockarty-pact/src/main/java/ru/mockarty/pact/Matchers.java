// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Static factory for Pact matchers.
 *
 * <p>This is the user-facing entry point — never construct {@link Matcher}
 * records directly. Each factory validates its inputs (regex compiles,
 * min&lt;=max, non-null example where required) so misuse surfaces at the
 * call site instead of at JSON-write time.</p>
 *
 * <p>V3 + V4 shared:
 * {@link #like}, {@link #term}, {@link #eachLike}, {@link #eachKeyLike},
 * {@link #regex}, {@link #integer}, {@link #decimal}, {@link #bool}.</p>
 *
 * <p>V4-only:
 * {@link #matchType}, {@link #minType}, {@link #maxType},
 * {@link #minMaxType}, {@link #arrayContains}, {@link #equality},
 * {@link #eachKey}, {@link #eachValue}.</p>
 */
public final class Matchers {

    private Matchers() {
        // Static utility — no instances.
    }

    // ── V3 + V4 shared ──────────────────────────────────────────────

    /** Type-matching: actual value must be same JSON type as {@code example}. */
    public static Matcher like(Object example) {
        Objects.requireNonNull(example, "like: example must not be null (use a placeholder if you really mean null)");
        return new Matcher.Like(example);
    }

    /** Regex-matching: actual string must match {@code regex}; {@code example} populates the body. */
    public static Matcher term(String regex, Object example) {
        Objects.requireNonNull(regex, "term: regex must not be null");
        Objects.requireNonNull(example, "term: example must not be null");
        compileGuard(regex);
        return new Matcher.Term(regex, example);
    }

    /** Array where every element matches the shape of {@code example}; min defaults to 1. */
    public static Matcher eachLike(Object example) {
        return eachLike(example, 1);
    }

    /** Array where every element matches the shape of {@code example}, with at least {@code min}. */
    public static Matcher eachLike(Object example, int min) {
        Objects.requireNonNull(example, "eachLike: example must not be null");
        if (min < 0) throw new IllegalArgumentException("eachLike: min must be >= 0, got " + min);
        return new Matcher.EachLike(example, min);
    }

    /** Map-key-shape match (V3 dialect, retained for compatibility). */
    public static Matcher eachKeyLike(Object example) {
        Objects.requireNonNull(example, "eachKeyLike: example must not be null");
        return new Matcher.EachKeyLike(example);
    }

    /** Plain regex matcher carrying its own example string. */
    public static Matcher regex(String pattern, String example) {
        Objects.requireNonNull(pattern, "regex: pattern must not be null");
        Objects.requireNonNull(example, "regex: example must not be null");
        compileGuard(pattern);
        return new Matcher.Regex(pattern, example);
    }

    /** Integer type matcher. */
    public static Matcher integer(Number example) {
        Objects.requireNonNull(example, "integer: example must not be null");
        return new Matcher.Integer(example);
    }

    /** Decimal type matcher. */
    public static Matcher decimal(Number example) {
        Objects.requireNonNull(example, "decimal: example must not be null");
        return new Matcher.Decimal(example);
    }

    /** Boolean type matcher. */
    public static Matcher bool(Boolean example) {
        Objects.requireNonNull(example, "bool: example must not be null");
        return new Matcher.Bool(example);
    }

    // ── V4-only ──────────────────────────────────────────────────────

    /** V4 matchType — explicit type-only match. */
    public static Matcher matchType(Object example) {
        Objects.requireNonNull(example, "matchType: example must not be null");
        return new Matcher.MatchType(example);
    }

    /** V4 minType — array of at least {@code min} elements. */
    public static Matcher minType(Object example, int min) {
        Objects.requireNonNull(example, "minType: example must not be null");
        if (min < 0) throw new IllegalArgumentException("minType: min must be >= 0, got " + min);
        return new Matcher.MinType(example, min);
    }

    /** V4 maxType — array of at most {@code max} elements. */
    public static Matcher maxType(Object example, int max) {
        Objects.requireNonNull(example, "maxType: example must not be null");
        if (max < 0) throw new IllegalArgumentException("maxType: max must be >= 0, got " + max);
        return new Matcher.MaxType(example, max);
    }

    /** V4 minMaxType — bounded array. */
    public static Matcher minMaxType(Object example, int min, int max) {
        Objects.requireNonNull(example, "minMaxType: example must not be null");
        if (min < 0) throw new IllegalArgumentException("minMaxType: min must be >= 0, got " + min);
        if (max < min) throw new IllegalArgumentException("minMaxType: max (" + max + ") must be >= min (" + min + ")");
        return new Matcher.MinMaxType(example, min, max);
    }

    /** V4 arrayContains — every {@code variant} must appear somewhere in the array. */
    public static Matcher arrayContains(Object... variants) {
        Objects.requireNonNull(variants, "arrayContains: variants must not be null");
        if (variants.length == 0) {
            throw new IllegalArgumentException("arrayContains: must declare at least one variant");
        }
        return new Matcher.ArrayContains(Arrays.asList(variants));
    }

    /** V4 equality — strict deep equality escape hatch. */
    public static Matcher equality(Object example) {
        Objects.requireNonNull(example, "equality: example must not be null");
        return new Matcher.Equality(example);
    }

    /** V4 eachKey — apply nested matcher rules to every map key. */
    public static Matcher eachKey(Object example, Matcher... rules) {
        Objects.requireNonNull(example, "eachKey: example must not be null");
        Objects.requireNonNull(rules, "eachKey: rules must not be null");
        if (rules.length == 0) {
            throw new IllegalArgumentException("eachKey: must declare at least one nested rule");
        }
        return new Matcher.EachKey(example, Arrays.asList(rules));
    }

    /** V4 eachValue — apply nested matcher rules to every map value. */
    public static Matcher eachValue(Object example, Matcher... rules) {
        Objects.requireNonNull(example, "eachValue: example must not be null");
        Objects.requireNonNull(rules, "eachValue: rules must not be null");
        if (rules.length == 0) {
            throw new IllegalArgumentException("eachValue: must declare at least one nested rule");
        }
        return new Matcher.EachValue(example, Arrays.asList(rules));
    }

    // ── Internals ────────────────────────────────────────────────────

    private static void compileGuard(String regex) {
        try {
            java.util.regex.Pattern.compile(regex);
        } catch (java.util.regex.PatternSyntaxException e) {
            throw new IllegalArgumentException(
                    "regex syntax error: " + regex + " — " + e.getDescription(), e);
        }
    }
}
