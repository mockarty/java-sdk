// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Decoded shape returned by
 * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report}.
 *
 * <p>The server keeps the payload loosely typed on purpose — new Allure
 * fields roll out server-side without SDK bumps. The raw bytes are kept
 * in {@link #getRaw()} so callers can run a second decode pass against
 * their own types.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllureReport {

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("items")
    private List<ItemSummary> items;

    @JsonProperty("summary")
    private Map<String, Object> summary;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonIgnore
    private byte[] raw;

    public AllureReport() {
    }

    public String getRunId() {
        return runId;
    }

    public String getPlanId() {
        return planId;
    }

    public String getStatus() {
        return status;
    }

    public List<ItemSummary> getItems() {
        return items;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Returns the raw JSON bytes the server emitted, preserved so callers
     * can reparse with a custom {@code ObjectMapper} / type hierarchy when
     * the server introduces fields the SDK doesn't yet recognise.
     */
    public byte[] getRaw() {
        return raw;
    }

    public AllureReport raw(byte[] raw) {
        this.raw = raw;
        return this;
    }
}
