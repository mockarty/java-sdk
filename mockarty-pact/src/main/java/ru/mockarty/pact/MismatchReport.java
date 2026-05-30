// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact;

import java.util.Objects;

/**
 * Structured matcher mismatch — emitted by {@link MatcherEngine} every
 * time a declared matcher fails against an actual value.
 *
 * <p>The shape is intentionally close to the {@code MismatchReport} that
 * Go and Python SDKs surface so cross-SDK CI tooling can normalise on a
 * single JSON shape. Field order follows the descending-alignment rule
 * from the project's struct-alignment guideline (records honour it too —
 * the JVM compiler reuses the declared order for field layout).</p>
 *
 * @param path        JSONPath-style location ({@code $.body.amount},
 *                    {@code $.header.X-Trace}, {@code $.path}, etc.)
 * @param expected    Human-readable rendering of the expected shape.
 * @param actual      Human-readable rendering of what arrived.
 * @param matcherType Matcher discriminator (e.g. {@code regex},
 *                    {@code equality}, {@code minType}).
 */
public record MismatchReport(
        String path,
        String expected,
        String actual,
        String matcherType) {

    public MismatchReport {
        Objects.requireNonNull(path, "path must not be null");
        Objects.requireNonNull(matcherType, "matcherType must not be null");
        // expected / actual may be null — represent as the literal string
        // "null" so test output never confuses "no value" with "value=null".
        if (expected == null) expected = "null";
        if (actual == null) actual = "null";
    }

    /** Single-line summary suitable for AssertionError messages. */
    public String toLine() {
        return "[" + matcherType + "] " + path
                + " — expected " + expected
                + ", actual " + actual;
    }
}
