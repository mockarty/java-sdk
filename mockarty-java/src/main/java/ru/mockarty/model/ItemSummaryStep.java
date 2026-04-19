// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One step inside an {@link ItemSummary} — maps to an Allure step.
 *
 * <p>Steps are recursive: a step may hold nested {@code steps} of the same
 * shape. The server emits ISO-8601 timestamps as strings; the SDK keeps
 * them as strings so callers can choose their own temporal library.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemSummaryStep {

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

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

    public ItemSummaryStep() {
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
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
}
