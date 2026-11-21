// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Aggregate execution of a Test Plan.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestPlanRun {

    @JsonProperty("id")
    private String id;

    @JsonProperty("runIds")
    private List<String> runIds;

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("status")
    private String status;

    @JsonProperty("triggeredBy")
    private String triggeredBy;

    @JsonProperty("reportUrl")
    private String reportUrl;

    @JsonProperty("itemsState")
    private List<PlanItemState> itemsState;

    @JsonProperty("totalItems")
    private int totalItems;

    @JsonProperty("completedItems")
    private int completedItems;

    @JsonProperty("failedItems")
    private int failedItems;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("completedAt")
    private String completedAt;

    public TestPlanRun() {
    }

    public String getId() {
        return id;
    }

    /** IDs of all runs returned for a configuration fan-out. */
    public List<String> getRunIds() {
        return runIds == null ? List.of() : List.copyOf(runIds);
    }

    public String getPlanId() {
        return planId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getStatus() {
        return status;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public List<PlanItemState> getItemsState() {
        return itemsState;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getCompletedItems() {
        return completedItems;
    }

    public int getFailedItems() {
        return failedItems;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    @Override
    public String toString() {
        return "TestPlanRun{id='" + id + "', status='" + status +
                "', total=" + totalItems + ", failed=" + failedItems + "}";
    }
}
