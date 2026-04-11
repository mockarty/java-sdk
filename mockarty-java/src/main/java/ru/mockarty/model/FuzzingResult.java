// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a completed fuzzing run, including findings and statistics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingResult {

    @JsonProperty("id")
    private String id;

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("configId")
    private String configId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("strategy")
    private String strategy;

    @JsonProperty("totalRequests")
    private Integer totalRequests;

    @JsonProperty("successCount")
    private Integer successCount;

    @JsonProperty("errorCount")
    private Integer errorCount;

    @JsonProperty("criticalFindings")
    private Integer criticalFindings;

    @JsonProperty("highFindings")
    private Integer highFindings;

    @JsonProperty("mediumFindings")
    private Integer mediumFindings;

    @JsonProperty("lowFindings")
    private Integer lowFindings;

    @JsonProperty("infoFindings")
    private Integer infoFindings;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("completedAt")
    private String completedAt;

    @JsonProperty("totalFindings")
    private Integer totalFindings;

    @JsonProperty("durationMs")
    private Long durationMs;

    @JsonProperty("namespace")
    private String namespace;

    public FuzzingResult() {
    }

    // Builder-style setters

    public FuzzingResult id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingResult runId(String runId) {
        this.runId = runId;
        return this;
    }

    public FuzzingResult configId(String configId) {
        this.configId = configId;
        return this;
    }

    public FuzzingResult status(String status) {
        this.status = status;
        return this;
    }

    public FuzzingResult strategy(String strategy) {
        this.strategy = strategy;
        return this;
    }

    public FuzzingResult totalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
        return this;
    }

    public FuzzingResult successCount(Integer successCount) {
        this.successCount = successCount;
        return this;
    }

    public FuzzingResult errorCount(Integer errorCount) {
        this.errorCount = errorCount;
        return this;
    }

    public FuzzingResult criticalFindings(Integer criticalFindings) {
        this.criticalFindings = criticalFindings;
        return this;
    }

    public FuzzingResult highFindings(Integer highFindings) {
        this.highFindings = highFindings;
        return this;
    }

    public FuzzingResult mediumFindings(Integer mediumFindings) {
        this.mediumFindings = mediumFindings;
        return this;
    }

    public FuzzingResult lowFindings(Integer lowFindings) {
        this.lowFindings = lowFindings;
        return this;
    }

    public FuzzingResult infoFindings(Integer infoFindings) {
        this.infoFindings = infoFindings;
        return this;
    }

    public FuzzingResult startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public FuzzingResult completedAt(String completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public FuzzingResult totalFindings(Integer totalFindings) {
        this.totalFindings = totalFindings;
        return this;
    }

    public FuzzingResult durationMs(Long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    public FuzzingResult namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getConfigId() {
        return configId;
    }

    public String getStatus() {
        return status;
    }

    public String getStrategy() {
        return strategy;
    }

    public Integer getTotalRequests() {
        return totalRequests;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public Integer getCriticalFindings() {
        return criticalFindings;
    }

    public Integer getHighFindings() {
        return highFindings;
    }

    public Integer getMediumFindings() {
        return mediumFindings;
    }

    public Integer getLowFindings() {
        return lowFindings;
    }

    public Integer getInfoFindings() {
        return infoFindings;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public Integer getTotalFindings() {
        return totalFindings;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "FuzzingResult{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", totalRequests=" + totalRequests +
                ", totalFindings=" + totalFindings +
                '}';
    }
}
