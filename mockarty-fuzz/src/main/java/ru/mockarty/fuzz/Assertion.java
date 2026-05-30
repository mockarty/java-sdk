// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.time.Duration;
import java.util.Objects;

/**
 * Server-side stop / fail condition the fuzz engine should evaluate
 * against each mutated response.
 *
 * <p>Sealed interface with one record per assertion kind — chosen over an
 * enum-with-switches per {@code feedback_dynamic_over_hardcode.md}. The
 * transpiler dispatches on the runtime type, so adding a new assertion
 * means adding a new record and one branch in
 * {@link Transpiler}; no string switches grow.</p>
 *
 * <p>Construct via the static factory methods —
 * {@link #statusInRange(int, int)}, {@link #noCrash()},
 * {@link #responseTimeUnder(Duration)}, {@link #noErrorInBody(String...)}.</p>
 */
public sealed interface Assertion
        permits Assertion.Status,
                Assertion.NoCrash,
                Assertion.ResponseTimeUnder,
                Assertion.NoErrorInBody {

    /** Wire identifier emitted into the JSON config's {@code type} field. */
    String type();

    /**
     * Asserts the response status is in the half-open range {@code [min, max]}
     * (both inclusive). Common shapes: {@code statusInRange(200, 299)} for
     * "success window", {@code statusInRange(500, 599)} treated as failure
     * (negated in the engine).
     */
    static Status statusInRange(int min, int max) {
        if (min < 100 || max < min || max > 599) {
            throw new IllegalArgumentException(
                    "statusInRange: invalid HTTP status window [" + min + ", " + max + "]");
        }
        return new Status(min, max);
    }

    /**
     * Asserts the server didn't crash (connection reset, no response,
     * read timeout, or 5xx). This is the baseline "I just want to see
     * if I can knock it over" assertion.
     */
    static NoCrash noCrash() {
        return NoCrashHolder.INSTANCE;
    }

    /**
     * Asserts the per-request response time is strictly less than
     * {@code limit}. Useful for catching algorithmic complexity bugs
     * (ReDoS, hash-collision DoS, unbounded recursion).
     */
    static ResponseTimeUnder responseTimeUnder(Duration limit) {
        Objects.requireNonNull(limit, "limit must not be null");
        if (limit.isZero() || limit.isNegative()) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return new ResponseTimeUnder(limit);
    }

    /**
     * Asserts the response body does NOT contain any of the supplied
     * substrings (case-sensitive). Typical {@code errorTokens}:
     * {@code "stack trace"}, {@code "SQLSTATE"}, {@code "Exception in"}.
     * An empty {@code errorTokens} array uses the engine's built-in
     * default error-marker corpus.
     */
    static NoErrorInBody noErrorInBody(String... errorTokens) {
        Objects.requireNonNull(errorTokens, "errorTokens must not be null");
        for (String token : errorTokens) {
            if (token == null || token.isBlank()) {
                throw new IllegalArgumentException(
                        "errorTokens must not contain null or blank entries");
            }
        }
        return new NoErrorInBody(errorTokens.clone());
    }

    /** Response status window assertion. */
    record Status(int min, int max) implements Assertion {
        @Override public String type() { return "status_in_range"; }
    }

    /** Crash / connection-failure assertion (no parameters). */
    record NoCrash() implements Assertion {
        @Override public String type() { return "no_crash"; }
    }

    /** Per-request latency cap assertion. */
    record ResponseTimeUnder(Duration limit) implements Assertion {
        @Override public String type() { return "response_time_under"; }
    }

    /** Body-substring banlist assertion. */
    record NoErrorInBody(String[] errorTokens) implements Assertion {
        @Override public String type() { return "no_error_in_body"; }
        // Defensive copy on the way out so callers can't mutate our state.
        public String[] errorTokens() { return errorTokens.clone(); }
    }

    /** Singleton wrapper — NoCrash has no fields, no need to allocate. */
    final class NoCrashHolder {
        private static final NoCrash INSTANCE = new NoCrash();
        private NoCrashHolder() {}
    }
}
