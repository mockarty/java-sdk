// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.pact.verifier;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated verification result — every interaction's outcome plus
 * timing.
 *
 * @param provider     provider name (from {@code Verifier.providerName})
 * @param startedAt    UTC instant when verification began
 * @param finishedAt   UTC instant when verification ended
 * @param interactions per-interaction results in pact order
 */
public record VerificationResult(
    String provider,
    Instant startedAt,
    Instant finishedAt,
    List<InteractionResult> interactions
) {
    public VerificationResult {
        if (interactions == null) interactions = List.of();
    }

    /** {@code true} iff every interaction passed. */
    public boolean ok() {
        if (interactions.isEmpty()) return false;
        for (InteractionResult ir : interactions) {
            if (!ir.passed()) return false;
        }
        return true;
    }

    /** One-line summary suitable for assertion failure messages. */
    public String summary() {
        int passed = 0;
        for (InteractionResult ir : interactions) if (ir.passed()) passed++;
        return passed + "/" + interactions.size() + " interactions passed";
    }
}
