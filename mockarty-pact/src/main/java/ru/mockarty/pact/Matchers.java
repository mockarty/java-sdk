// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

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

    /**
     * V4 jsonPath — apply {@code inner} to the JSON value located at
     * {@code path} (root {@code $}, dot-keys, {@code [index]} segments).
     */
    public static Matcher jsonPath(String path, Matcher inner) {
        Objects.requireNonNull(path, "jsonPath: path must not be null");
        Objects.requireNonNull(inner, "jsonPath: inner matcher must not be null");
        if (path.isBlank()) {
            throw new IllegalArgumentException("jsonPath: path must not be blank");
        }
        return new Matcher.JsonPath(path, inner);
    }

    /**
     * V4 xmlPath — apply {@code inner} to the XML value located at
     * {@code xpath} (XPath 1.0). Compilation is validated eagerly so a
     * malformed expression surfaces at DSL time, not at request time.
     */
    public static Matcher xmlPath(String xpath, Matcher inner) {
        Objects.requireNonNull(xpath, "xmlPath: xpath must not be null");
        Objects.requireNonNull(inner, "xmlPath: inner matcher must not be null");
        if (xpath.isBlank()) {
            throw new IllegalArgumentException("xmlPath: xpath must not be blank");
        }
        try {
            javax.xml.xpath.XPathFactory.newInstance().newXPath().compile(xpath);
        } catch (javax.xml.xpath.XPathExpressionException e) {
            throw new IllegalArgumentException(
                    "xmlPath: invalid XPath expression: " + xpath + " — " + e.getMessage(), e);
        }
        return new Matcher.XmlPath(xpath, inner);
    }

    // ── Scalar & format matchers (server-parity catalogue) ───────────

    /** Matches any value that is not {@code null}. */
    public static Matcher notNull(Object example) {
        Objects.requireNonNull(example, "notNull: example must not be null");
        return new Matcher.NotNull(example);
    }

    /** Matches a string that contains {@code substring}. */
    public static Matcher include(String substring) {
        Objects.requireNonNull(substring, "include: substring must not be null");
        return new Matcher.Include(substring, substring);
    }

    /** Matches a string whose value starts with {@code contentType}. */
    public static Matcher contentType(String contentType) {
        Objects.requireNonNull(contentType, "contentType: value must not be null");
        return new Matcher.ContentType(contentType, contentType);
    }

    /** Matches an array that contains at least one element. The {@code example}
     * is rendered as the single-element array preview the verifier replays. */
    public static Matcher atLeastOne(Object example) {
        Objects.requireNonNull(example, "atLeastOne: example must not be null");
        return new Matcher.AtLeastOne(List.of(example));
    }

    /** Matches an ISO date string (e.g. {@code 2026-06-12}). */
    public static Matcher date(String example) { return format("date", null, example); }

    /** {@link #date} with a caller-supplied regex override. */
    public static Matcher date(String example, String regex) { return format("date", regex, example); }

    /** Matches a {@code HH:MM:SS[.fraction]} string. */
    public static Matcher time(String example) { return format("time", null, example); }

    /** {@link #time} with a caller-supplied regex override. */
    public static Matcher time(String example, String regex) { return format("time", regex, example); }

    /** Matches an RFC 3339 / ISO 8601 timestamp (e.g. {@code 2026-06-12T10:30:00Z}). */
    public static Matcher dateTime(String example) { return format("timestamp", null, example); }

    /** {@link #dateTime} with a caller-supplied regex override. */
    public static Matcher dateTime(String example, String regex) { return format("timestamp", regex, example); }

    /** Alias of {@link #dateTime} for pact-jvm parity. */
    public static Matcher timestamp(String example) { return dateTime(example); }

    /** Matches a canonical UUID string. */
    public static Matcher uuid(String example) { return format("uuid", null, example); }

    /** {@link #uuid} with a caller-supplied regex override. */
    public static Matcher uuid(String example, String regex) { return format("uuid", regex, example); }

    /** Matches a SemVer 2.0 version string (e.g. {@code 1.2.3}). */
    public static Matcher semver(String example) { return format("semver", null, example); }

    /** Matches a dotted-quad IPv4 address string. */
    public static Matcher ipv4(String example) { return format("ipv4", null, example); }

    private static Matcher format(String matchName, String regex, Object example) {
        Objects.requireNonNull(example, matchName + ": example must not be null");
        if (regex != null) compileGuard(regex);
        return new Matcher.Format(matchName, regex, example);
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
