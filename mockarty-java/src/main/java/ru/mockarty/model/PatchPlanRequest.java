// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Partial-update payload for
 * {@code PATCH /api/v1/namespaces/:namespace/test-plans/:idOrNumericID}.
 *
 * <p>Each field is independently nullable: {@code null} means "no change",
 * a non-null value (including the empty string or {@code false}) overwrites
 * the server's current value.</p>
 *
 * <p>{@code scheduleCron} accepts either a 5- or 6-field cron expression
 * OR one of the sentinel modes {@code parallel} / {@code dag} — the server
 * validates both forms. Prefer {@code executionMode} for the typed mode
 * field; {@code scheduleCron} stays supported for cron schedules and
 * legacy clients (post-release contract).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatchPlanRequest {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("schedule_cron")
    private String scheduleCron;

    @JsonProperty("execution_mode")
    private String executionMode;

    @JsonProperty("enabled")
    private Boolean enabled;

    public PatchPlanRequest() {
    }

    public PatchPlanRequest name(String name) {
        this.name = name;
        return this;
    }

    public PatchPlanRequest description(String description) {
        this.description = description;
        return this;
    }

    public PatchPlanRequest scheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
        return this;
    }

    public PatchPlanRequest executionMode(String executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    public PatchPlanRequest enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * Returns {@code true} when every field is {@code null} — i.e. calling
     * {@code patch} would send an empty body, which the server rejects.
     */
    public boolean isEmpty() {
        return name == null && description == null && scheduleCron == null
                && executionMode == null && enabled == null;
    }
}
