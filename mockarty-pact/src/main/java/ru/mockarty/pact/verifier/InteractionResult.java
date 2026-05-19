// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.verifier;

import java.util.List;

/**
 * Per-interaction outcome of a verification run.
 *
 * @param description interaction's {@code description} from the pact
 * @param state       first provider state name (empty if none)
 * @param statusCode  HTTP status the provider returned (0 on error)
 * @param passed      whether the actual response matched the expected
 * @param error       non-empty when a setup / transport / filter step failed
 * @param mismatches  structured per-field mismatches (empty when passed)
 */
public record InteractionResult(
    String description,
    String state,
    int statusCode,
    boolean passed,
    String error,
    List<Mismatch> mismatches
) {
    public InteractionResult {
        if (description == null) description = "";
        if (state == null) state = "";
        if (error == null) error = "";
        if (mismatches == null) mismatches = List.of();
    }

    static InteractionResult error(String desc, String state, String err) {
        return new InteractionResult(desc, state, 0, false, err, List.of());
    }
}
