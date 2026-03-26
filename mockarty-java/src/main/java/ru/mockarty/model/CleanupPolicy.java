// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cleanup policy configuration for automatic resource cleanup.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CleanupPolicy {

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("retentionDays")
    private Integer retentionDays;

    @JsonProperty("cleanupDeletedMocks")
    private Boolean cleanupDeletedMocks;

    @JsonProperty("cleanupExpiredMocks")
    private Boolean cleanupExpiredMocks;

    @JsonProperty("cleanupLogs")
    private Boolean cleanupLogs;

    @JsonProperty("logRetentionDays")
    private Integer logRetentionDays;

    @JsonProperty("cleanupTestReports")
    private Boolean cleanupTestReports;

    @JsonProperty("testReportRetentionDays")
    private Integer testReportRetentionDays;

    @JsonProperty("cleanupFuzzResults")
    private Boolean cleanupFuzzResults;

    @JsonProperty("fuzzResultRetentionDays")
    private Integer fuzzResultRetentionDays;

    @JsonProperty("schedule")
    private String schedule;

    public CleanupPolicy() {
    }

    // Builder-style setters

    public CleanupPolicy enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public CleanupPolicy retentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
        return this;
    }

    public CleanupPolicy cleanupDeletedMocks(Boolean cleanupDeletedMocks) {
        this.cleanupDeletedMocks = cleanupDeletedMocks;
        return this;
    }

    public CleanupPolicy cleanupExpiredMocks(Boolean cleanupExpiredMocks) {
        this.cleanupExpiredMocks = cleanupExpiredMocks;
        return this;
    }

    public CleanupPolicy cleanupLogs(Boolean cleanupLogs) {
        this.cleanupLogs = cleanupLogs;
        return this;
    }

    public CleanupPolicy logRetentionDays(Integer logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
        return this;
    }

    public CleanupPolicy cleanupTestReports(Boolean cleanupTestReports) {
        this.cleanupTestReports = cleanupTestReports;
        return this;
    }

    public CleanupPolicy testReportRetentionDays(Integer testReportRetentionDays) {
        this.testReportRetentionDays = testReportRetentionDays;
        return this;
    }

    public CleanupPolicy cleanupFuzzResults(Boolean cleanupFuzzResults) {
        this.cleanupFuzzResults = cleanupFuzzResults;
        return this;
    }

    public CleanupPolicy fuzzResultRetentionDays(Integer fuzzResultRetentionDays) {
        this.fuzzResultRetentionDays = fuzzResultRetentionDays;
        return this;
    }

    public CleanupPolicy schedule(String schedule) {
        this.schedule = schedule;
        return this;
    }

    // Getters

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public Boolean getCleanupDeletedMocks() {
        return cleanupDeletedMocks;
    }

    public Boolean getCleanupExpiredMocks() {
        return cleanupExpiredMocks;
    }

    public Boolean getCleanupLogs() {
        return cleanupLogs;
    }

    public Integer getLogRetentionDays() {
        return logRetentionDays;
    }

    public Boolean getCleanupTestReports() {
        return cleanupTestReports;
    }

    public Integer getTestReportRetentionDays() {
        return testReportRetentionDays;
    }

    public Boolean getCleanupFuzzResults() {
        return cleanupFuzzResults;
    }

    public Integer getFuzzResultRetentionDays() {
        return fuzzResultRetentionDays;
    }

    public String getSchedule() {
        return schedule;
    }

    @Override
    public String toString() {
        return "CleanupPolicy{" +
                "enabled=" + enabled +
                ", retentionDays=" + retentionDays +
                '}';
    }
}
