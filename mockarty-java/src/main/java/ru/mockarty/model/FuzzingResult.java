// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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

    @JsonProperty("totalRequests")
    private Integer totalRequests;

    @JsonProperty("successCount")
    private Integer successCount;

    @JsonProperty("errorCount")
    private Integer errorCount;

    @JsonProperty("findings")
    private List<Map<String, Object>> findings;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("duration")
    private Long duration;

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

    public FuzzingResult findings(List<Map<String, Object>> findings) {
        this.findings = findings;
        return this;
    }

    public FuzzingResult startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public FuzzingResult finishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public FuzzingResult duration(Long duration) {
        this.duration = duration;
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

    public Integer getTotalRequests() {
        return totalRequests;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public List<Map<String, Object>> getFindings() {
        return findings;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public Long getDuration() {
        return duration;
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
                ", findings=" + (findings != null ? findings.size() : 0) +
                '}';
    }
}
