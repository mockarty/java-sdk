// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.exception;

import ru.mockarty.model.ExternalRunResponse;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when some results of an {@code allure-results} directory upload never
 * reached Mockarty.
 *
 * <p>The upload is best-effort per file so one malformed {@code *-result.json}
 * does not kill a CI upload — but the caller must still learn that N results
 * were dropped. Returning only the successes made a half-lost upload
 * indistinguishable from a clean one.</p>
 *
 * <p>{@link #getResults()} carries the responses that DID land, so a caller
 * that genuinely wants best-effort semantics can still use them after catching
 * this.</p>
 */
public class AllureUploadPartialException extends MockartyException {

    private final transient List<ExternalRunResponse> results;
    private final transient List<String> skipped;

    public AllureUploadPartialException(List<ExternalRunResponse> results, List<String> skipped) {
        super("mockarty: " + (results == null ? 0 : results.size())
                + " result(s) uploaded, " + (skipped == null ? 0 : skipped.size())
                + " not reported: " + String.join("; ", skipped == null ? List.of() : skipped));
        this.results = results == null ? List.of() : Collections.unmodifiableList(results);
        this.skipped = skipped == null ? List.of() : Collections.unmodifiableList(skipped);
    }

    /** Responses for the results that were uploaded successfully. */
    public List<ExternalRunResponse> getResults() {
        return results;
    }

    /** One {@code "<file>: <reason>"} entry per result that was not reported. */
    public List<String> getSkipped() {
        return skipped;
    }

    /** Number of results that reached Mockarty. */
    public int getUploaded() {
        return results.size();
    }
}
