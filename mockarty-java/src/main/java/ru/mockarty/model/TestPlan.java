// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a Test Plan — master orchestrator for heterogeneous runs
 * (functional / load / fuzz / chaos / contract).
 *
 * <p>See {@code docs/research/TEST_PLANS_ARCHITECTURE_2026-04-19.md} for the
 * full specification.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestPlan {

    @JsonProperty("id")
    private String id;

    @JsonProperty("numericId")
    private Long numericId;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    /** Legacy "mode" column — kept for backward-compat (cron + parallel/dag sentinels). */
    @JsonProperty("schedule")
    private String schedule;

    /** Typed plan-level execution mode introduced in migration 077.
     *  One of {@code "fifo"}, {@code "parallel"}, {@code "dag"}. Empty defers
     *  to server auto-detect (Gates → DAG, otherwise FIFO). Prefer this over
     *  the schedule sentinel forms; cron expressions still belong in
     *  {@link #schedule}. */
    @JsonProperty("executionMode")
    private String executionMode;

    @JsonProperty("items")
    private List<TestPlanItem> items;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("closedAt")
    private String closedAt;

    @JsonProperty("createdBy")
    private String createdBy;

    public TestPlan() {
    }

    public TestPlan id(String id) {
        this.id = id;
        return this;
    }

    public TestPlan numericId(Long numericId) {
        this.numericId = numericId;
        return this;
    }

    public TestPlan namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public TestPlan name(String name) {
        this.name = name;
        return this;
    }

    public TestPlan description(String description) {
        this.description = description;
        return this;
    }

    public TestPlan schedule(String schedule) {
        this.schedule = schedule;
        return this;
    }

    public TestPlan executionMode(String executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    public TestPlan items(List<TestPlanItem> items) {
        this.items = items;
        return this;
    }

    public String getId() {
        return id;
    }

    public Long getNumericId() {
        return numericId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    /** Canonical execution-mode constants (mirrors server testplan.ExecutionMode*). */
    public static final String EXECUTION_MODE_FIFO = "fifo";
    public static final String EXECUTION_MODE_PARALLEL = "parallel";
    public static final String EXECUTION_MODE_DAG = "dag";

    public List<TestPlanItem> getItems() {
        return items;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getClosedAt() {
        return closedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return "TestPlan{id='" + id + "', name='" + name + "', items=" +
                (items != null ? items.size() : 0) + "}";
    }
}
