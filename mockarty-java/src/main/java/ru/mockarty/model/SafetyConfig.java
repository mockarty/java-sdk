// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Safety guardrails configuration for a chaos experiment.
 *
 * <p>Maps to the server-side SafetyConfig struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SafetyConfig {

    @JsonProperty("denyNamespaces")
    private List<String> denyNamespaces;

    @JsonProperty("allowNamespaces")
    private List<String> allowNamespaces;

    @JsonProperty("maxConcurrent")
    private Integer maxConcurrent;

    @JsonProperty("maxPodsAffected")
    private Integer maxPodsAffected;

    @JsonProperty("minReplicasAlive")
    private Integer minReplicasAlive;

    @JsonProperty("autoRollback")
    private Boolean autoRollback;

    @JsonProperty("haltOnSteadyFail")
    private Boolean haltOnSteadyFail;

    @JsonProperty("requireApproval")
    private Boolean requireApproval;

    @JsonProperty("blastRadiusPercent")
    private Integer blastRadiusPercent;

    public SafetyConfig() {
    }

    // Builder-style setters

    public SafetyConfig denyNamespaces(List<String> denyNamespaces) {
        this.denyNamespaces = denyNamespaces;
        return this;
    }

    public SafetyConfig allowNamespaces(List<String> allowNamespaces) {
        this.allowNamespaces = allowNamespaces;
        return this;
    }

    public SafetyConfig maxConcurrent(Integer maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
        return this;
    }

    public SafetyConfig maxPodsAffected(Integer maxPodsAffected) {
        this.maxPodsAffected = maxPodsAffected;
        return this;
    }

    public SafetyConfig minReplicasAlive(Integer minReplicasAlive) {
        this.minReplicasAlive = minReplicasAlive;
        return this;
    }

    public SafetyConfig autoRollback(Boolean autoRollback) {
        this.autoRollback = autoRollback;
        return this;
    }

    public SafetyConfig haltOnSteadyFail(Boolean haltOnSteadyFail) {
        this.haltOnSteadyFail = haltOnSteadyFail;
        return this;
    }

    public SafetyConfig requireApproval(Boolean requireApproval) {
        this.requireApproval = requireApproval;
        return this;
    }

    public SafetyConfig blastRadiusPercent(Integer blastRadiusPercent) {
        this.blastRadiusPercent = blastRadiusPercent;
        return this;
    }

    // Getters

    public List<String> getDenyNamespaces() {
        return denyNamespaces;
    }

    public List<String> getAllowNamespaces() {
        return allowNamespaces;
    }

    public Integer getMaxConcurrent() {
        return maxConcurrent;
    }

    public Integer getMaxPodsAffected() {
        return maxPodsAffected;
    }

    public Integer getMinReplicasAlive() {
        return minReplicasAlive;
    }

    public Boolean getAutoRollback() {
        return autoRollback;
    }

    public Boolean getHaltOnSteadyFail() {
        return haltOnSteadyFail;
    }

    public Boolean getRequireApproval() {
        return requireApproval;
    }

    public Integer getBlastRadiusPercent() {
        return blastRadiusPercent;
    }

    @Override
    public String toString() {
        return "SafetyConfig{" +
                "maxConcurrent=" + maxConcurrent +
                ", maxPodsAffected=" + maxPodsAffected +
                ", autoRollback=" + autoRollback +
                '}';
    }
}
