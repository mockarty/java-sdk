// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz;

/**
 * A live event emitted by a running fuzz job.
 *
 * <p>Stream of these comes back from {@link Runner#stream(JobId)} —
 * SSE-driven on the server side. Sealed because there are only three
 * shapes the engine ever publishes; pattern-match on them in user code
 * with an exhaustive switch.</p>
 */
public sealed interface Event permits Event.Progress, Event.FindingFound, Event.Completed {

    /**
     * Periodic progress heartbeat. Mirrors
     * {@code internal/fuzzing/config.go ProgressInfo}.
     */
    record Progress(
            long completedRequests,
            long totalRequests,
            double requestsPerSecond,
            int criticalFindings,
            int highFindings,
            int mediumFindings,
            int lowFindings,
            int infoFindings
    ) implements Event {}

    /**
     * Fired the instant a new finding is added to the run. Useful to
     * stream into a live dashboard or to short-circuit a CI job on the
     * first critical hit (when {@code stopOnFinding} isn't already set).
     */
    record FindingFound(Finding finding) implements Event {
        public FindingFound {
            if (finding == null) throw new IllegalArgumentException("finding must not be null");
        }
    }

    /** Terminal event — the run has finished (gracefully or otherwise). */
    record Completed(Result result) implements Event {
        public Completed {
            if (result == null) throw new IllegalArgumentException("result must not be null");
        }
    }
}
