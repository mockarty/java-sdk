// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.verifier;

/**
 * A single field-level mismatch between an interaction's expected
 * response and the response the provider actually returned.
 *
 * <p>Distinct from {@link ru.mockarty.pact.MismatchReport} (which is
 * the consumer-side mock-server's request-matching report) — the
 * verifier inspects response shapes, not request shapes, so it needs
 * its own structured value with {@code Object} expected/actual to
 * preserve numeric / boolean / array types in published results.</p>
 *
 * @param path     JSONPath-style location, e.g. {@code $.body.id}
 * @param reason   short human-readable explanation
 * @param expected the expected value, or {@code null} if "missing"
 * @param actual   the actual value, or {@code null} if "missing"
 */
public record Mismatch(String path, String reason, Object expected, Object actual) {
}
