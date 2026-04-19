// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Native Mockarty-shape report served by
 * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.unified.json}.
 *
 * <p>The server serialises the same fields for every language SDK (Go /
 * Python / Java). {@link #getRaw()} preserves the wire bytes so callers
 * can decode into custom types if the server adds new fields before the
 * SDK is updated.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedReport {

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("planName")
    private String planName;

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("results")
    private List<UnifiedItemResult> results;

    @JsonProperty("counts")
    private UnifiedReportCounts counts;

    @JsonProperty("generatedAt")
    private Long generatedAt;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonIgnore
    private byte[] raw;

    public UnifiedReport() {
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getPlanName() {
        return planName;
    }

    public String getRunId() {
        return runId;
    }

    public List<UnifiedItemResult> getResults() {
        return results;
    }

    public UnifiedReportCounts getCounts() {
        return counts;
    }

    public Long getGeneratedAt() {
        return generatedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    /**
     * Returns the raw JSON bytes the server emitted, preserved so callers
     * can reparse with a custom {@code ObjectMapper} / type hierarchy when
     * the server introduces fields the SDK does not yet recognise.
     */
    public byte[] getRaw() {
        return raw;
    }

    public UnifiedReport raw(byte[] raw) {
        this.raw = raw;
        return this;
    }
}
