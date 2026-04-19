// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * State of one item within a running Test Plan.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanItemState {

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("skipReason")
    private String skipReason;

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("completedAt")
    private String completedAt;

    @JsonProperty("summary")
    private Map<String, Object> summary;

    public PlanItemState() {
    }

    public Integer getOrder() {
        return order;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public String getRunId() {
        return runId;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public Map<String, Object> getSummary() {
        return summary;
    }
}
