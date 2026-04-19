// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One firing rule for a Test Plan (cron / once / interval).
 *
 * <p>Named {@code PlanSchedule} to avoid clashing with
 * {@link ScheduleConfig} (chaos schedule descriptor).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanSchedule {

    @JsonProperty("id")
    private String id;

    @JsonProperty("planId")
    private String planId;

    /** One of {@code cron}, {@code once}, {@code interval}. */
    @JsonProperty("kind")
    private String kind;

    @JsonProperty("cronExpr")
    private String cronExpr;

    @JsonProperty("runAt")
    private String runAt;

    @JsonProperty("intervalSeconds")
    private Integer intervalSeconds;

    @JsonProperty("timezone")
    private String timezone;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("lastFiredAt")
    private String lastFiredAt;

    @JsonProperty("nextFireAt")
    private String nextFireAt;

    public PlanSchedule() {
    }

    public PlanSchedule id(String id) {
        this.id = id;
        return this;
    }

    public PlanSchedule kind(String kind) {
        this.kind = kind;
        return this;
    }

    public PlanSchedule cronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
        return this;
    }

    public PlanSchedule runAt(String runAt) {
        this.runAt = runAt;
        return this;
    }

    public PlanSchedule intervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
        return this;
    }

    public PlanSchedule timezone(String timezone) {
        this.timezone = timezone;
        return this;
    }

    public PlanSchedule enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getKind() {
        return kind;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public String getRunAt() {
        return runAt;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public String getTimezone() {
        return timezone;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getLastFiredAt() {
        return lastFiredAt;
    }

    public String getNextFireAt() {
        return nextFireAt;
    }
}
