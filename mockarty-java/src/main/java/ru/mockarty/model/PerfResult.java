// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Result of a performance test run.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfResult {

    @JsonProperty("id")
    private String id;

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

    @JsonProperty("avgLatencyMs")
    private Double avgLatencyMs;

    @JsonProperty("p95LatencyMs")
    private Double p95LatencyMs;

    @JsonProperty("p99LatencyMs")
    private Double p99LatencyMs;

    @JsonProperty("rps")
    private Double rps;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public PerfResult() {
    }

    // Builder-style setters

    public PerfResult id(String id) {
        this.id = id;
        return this;
    }

    public PerfResult configId(String configId) {
        this.configId = configId;
        return this;
    }

    public PerfResult status(String status) {
        this.status = status;
        return this;
    }

    public PerfResult totalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
        return this;
    }

    public PerfResult successCount(Integer successCount) {
        this.successCount = successCount;
        return this;
    }

    public PerfResult errorCount(Integer errorCount) {
        this.errorCount = errorCount;
        return this;
    }

    public PerfResult avgLatencyMs(Double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
        return this;
    }

    public PerfResult p95LatencyMs(Double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
        return this;
    }

    public PerfResult p99LatencyMs(Double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
        return this;
    }

    public PerfResult rps(Double rps) {
        this.rps = rps;
        return this;
    }

    public PerfResult startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public PerfResult finishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public PerfResult duration(Long duration) {
        this.duration = duration;
        return this;
    }

    public PerfResult metadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    // Getters

    public String getId() {
        return id;
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

    public Double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public Double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public Double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public Double getRps() {
        return rps;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "PerfResult{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", totalRequests=" + totalRequests +
                ", rps=" + rps +
                '}';
    }
}
