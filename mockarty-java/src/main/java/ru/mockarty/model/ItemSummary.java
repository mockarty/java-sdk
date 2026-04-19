// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Unified per-item report of a plan run. Mirrors the server-side
 * {@code internal/testplan.ItemSummary} so SDK callers can decode
 * {@code PlanItemState.summary} without writing a second parser.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemSummary {

    @JsonProperty("status")
    private String status;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("steps")
    private List<ItemSummaryStep> steps;

    @JsonProperty("attachments")
    private List<ItemSummaryAttachment> attachments;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("parameters")
    private Map<String, String> parameters;

    @JsonProperty("metrics")
    private Map<String, Double> metrics;

    public ItemSummary() {
    }

    public String getStatus() {
        return status;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public List<ItemSummaryStep> getSteps() {
        return steps;
    }

    public List<ItemSummaryAttachment> getAttachments() {
        return attachments;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public Map<String, Double> getMetrics() {
        return metrics;
    }
}
