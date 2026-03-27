// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Controls the scheduling behavior of a chaos experiment.
 *
 * <p>Maps to the server-side ScheduleConfig struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleConfig {

    @JsonProperty("cronExpr")
    private String cronExpr;

    @JsonProperty("repeatCount")
    private Integer repeatCount;

    @JsonProperty("jitter")
    private Integer jitter;

    public ScheduleConfig() {
    }

    // Builder-style setters

    public ScheduleConfig cronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
        return this;
    }

    public ScheduleConfig repeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
        return this;
    }

    public ScheduleConfig jitter(Integer jitter) {
        this.jitter = jitter;
        return this;
    }

    // Getters

    public String getCronExpr() {
        return cronExpr;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public Integer getJitter() {
        return jitter;
    }

    @Override
    public String toString() {
        return "ScheduleConfig{" +
                "cronExpr='" + cronExpr + '\'' +
                ", repeatCount=" + repeatCount +
                ", jitter=" + jitter +
                '}';
    }
}
