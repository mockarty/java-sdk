// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

import java.util.List;

/**
 * Final result of a completed fuzz run.
 *
 * <p>Mirror of {@code internal/fuzzing/config.go FuzzResult} flattened to
 * just what programmatic callers usually need: the status, the
 * findings-by-severity counts, and the full {@link Finding} list. Empty
 * lists rather than nulls — saves a defensive check on every iteration.</p>
 *
 * <p>{@code status} is the server-side life-cycle marker:
 * {@code "queued"}, {@code "running"}, {@code "completed"},
 * {@code "failed"}, {@code "cancelled"}, or {@code "timeout"}. Callers
 * checking "did the run finish successfully" should compare against
 * {@code "completed"} explicitly — a non-zero finding count is NOT a
 * failure (it's the engine doing its job).</p>
 */
public record Result(
        JobId jobId,
        String status,
        long durationMs,
        long totalRequests,
        int totalFindings,
        int criticalFindings,
        int highFindings,
        int mediumFindings,
        int lowFindings,
        int infoFindings,
        List<Finding> findings
) {
    public Result {
        // Defensive copy + null-coercion at the boundary.
        findings = findings == null ? List.of() : List.copyOf(findings);
    }
}
