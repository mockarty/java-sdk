// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Compact status payload returned by {@code /runs/:runId/status}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanRunStatus {

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalItems")
    private int totalItems;

    @JsonProperty("completedItems")
    private int completedItems;

    @JsonProperty("failedItems")
    private int failedItems;

    public PlanRunStatus() {
    }

    public String getStatus() {
        return status;
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
}
