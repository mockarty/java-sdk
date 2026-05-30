// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz;

/**
 * One discovered issue from a fuzz run.
 *
 * <p>Compact mirror of {@code internal/fuzzing/config.go Finding} —
 * carries only the fields a programmatic caller typically cares about
 * (severity, category, the offending request/response). The full server
 * record (LLM analysis, dedup IDs, baseline matches, etc.) is accessible
 * via {@code FuzzingApi.getFinding} when needed.</p>
 *
 * <p>{@code severity}: one of {@code "critical"}, {@code "high"},
 * {@code "medium"}, {@code "low"}, {@code "info"} (matches the
 * {@code SeverityCritical}..{@code SeverityInfo} constants on the server).</p>
 *
 * <p>{@code category}: one of the {@code Category*} constants on the
 * server side ({@code "sqli"}, {@code "xss"}, {@code "auth_bypass"},
 * etc.). We don't enumerate them here because the server's catalogue
 * grows; users matching on category should compare against the wire
 * string verbatim.</p>
 */
public record Finding(
        String id,
        String severity,
        String category,
        String title,
        String requestMethod,
        String requestUrl,
        int responseStatus,
        long responseTimeMs
) {}
