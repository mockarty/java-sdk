// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Aggregated results from a completed chaos experiment.
 *
 * <p>Maps to the server-side ChaosResults struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChaosResults {

    @JsonProperty("steadyStateBefore")
    private Boolean steadyStateBefore;

    @JsonProperty("steadyStateAfter")
    private Boolean steadyStateAfter;

    @JsonProperty("steadyStateDuring")
    private Boolean steadyStateDuring;

    @JsonProperty("phases")
    private List<Object> phases;

    @JsonProperty("timeline")
    private List<Object> timeline;

    @JsonProperty("affectedResources")
    private List<Object> affectedResources;

    @JsonProperty("totalDurationMs")
    private Long totalDurationMs;

    @JsonProperty("errorCount")
    private Integer errorCount;

    @JsonProperty("recoveryTimeMs")
    private Long recoveryTimeMs;

    @JsonProperty("resilienceScore")
    private Integer resilienceScore;

    @JsonProperty("summary")
    private String summary;

    public ChaosResults() {
    }

    // Getters

    public Boolean getSteadyStateBefore() {
        return steadyStateBefore;
    }

    public Boolean getSteadyStateAfter() {
        return steadyStateAfter;
    }

    public Boolean getSteadyStateDuring() {
        return steadyStateDuring;
    }

    public List<Object> getPhases() {
        return phases;
    }

    public List<Object> getTimeline() {
        return timeline;
    }

    public List<Object> getAffectedResources() {
        return affectedResources;
    }

    public Long getTotalDurationMs() {
        return totalDurationMs;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public Long getRecoveryTimeMs() {
        return recoveryTimeMs;
    }

    public Integer getResilienceScore() {
        return resilienceScore;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return "ChaosResults{" +
                "resilienceScore=" + resilienceScore +
                ", errorCount=" + errorCount +
                ", summary='" + summary + '\'' +
                '}';
    }
}
