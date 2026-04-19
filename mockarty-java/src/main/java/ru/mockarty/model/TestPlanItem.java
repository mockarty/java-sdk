// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single step inside a {@link TestPlan}. The {@code resourceId} points to
 * the source entity; its type is determined by {@code type}
 * ({@code functional} / {@code load} / {@code fuzz} / {@code chaos} /
 * {@code contract} / {@code test_plan}).
 *
 * <p>{@code test_plan} items reference another Test Plan by its ID — the
 * server runs the referenced plan as a child run and maps its terminal
 * status onto this item. Cycles across plans are rejected at save-time
 * with a 400 carrying a trace like {@code A → B → C → A}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestPlanItem {

    /** Canonical item types accepted by the server. */
    public static final String TYPE_FUNCTIONAL = "functional";
    public static final String TYPE_LOAD = "load";
    public static final String TYPE_FUZZ = "fuzz";
    public static final String TYPE_CHAOS = "chaos";
    public static final String TYPE_CONTRACT = "contract";
    public static final String TYPE_TEST_PLAN = "test_plan";

    @JsonProperty("order")
    private Integer order;

    @JsonProperty("type")
    private String type;

    @JsonProperty("refId")
    private String resourceId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("dependsOn")
    private List<String> dependsOn;

    @JsonProperty("startOffsetMs")
    private Long startOffsetMs;

    @JsonProperty("delayAfterMs")
    private Long delayAfterMs;

    public TestPlanItem() {
    }

    public TestPlanItem order(Integer order) {
        this.order = order;
        return this;
    }

    public TestPlanItem type(String type) {
        this.type = type;
        return this;
    }

    public TestPlanItem resourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public TestPlanItem name(String name) {
        this.name = name;
        return this;
    }

    public TestPlanItem dependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
        return this;
    }

    public TestPlanItem startOffsetMs(Long startOffsetMs) {
        this.startOffsetMs = startOffsetMs;
        return this;
    }

    public TestPlanItem delayAfterMs(Long delayAfterMs) {
        this.delayAfterMs = delayAfterMs;
        return this;
    }

    public Integer getOrder() {
        return order;
    }

    public String getType() {
        return type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getName() {
        return name;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public Long getStartOffsetMs() {
        return startOffsetMs;
    }

    public Long getDelayAfterMs() {
        return delayAfterMs;
    }
}
