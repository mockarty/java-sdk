// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a chaos engineering experiment.
 *
 * <p>Maps to the server-side ChaosExperiment struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChaosExperiment {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("presetName")
    private String presetName;

    @JsonProperty("faults")
    private List<FaultConfig> faults;

    @JsonProperty("target")
    private TargetConfig target;

    @JsonProperty("steadyState")
    private SteadyState steadyState;

    @JsonProperty("schedule")
    private ScheduleConfig schedule;

    @JsonProperty("safety")
    private SafetyConfig safety;

    @JsonProperty("results")
    private ChaosResults results;

    @JsonProperty("durationSec")
    private Integer durationSec;

    @JsonProperty("warmupSec")
    private Integer warmupSec;

    @JsonProperty("cooldownSec")
    private Integer cooldownSec;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("endedAt")
    private String endedAt;

    public ChaosExperiment() {
    }

    // Builder-style setters

    public ChaosExperiment id(String id) {
        this.id = id;
        return this;
    }

    public ChaosExperiment name(String name) {
        this.name = name;
        return this;
    }

    public ChaosExperiment namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ChaosExperiment description(String description) {
        this.description = description;
        return this;
    }

    public ChaosExperiment status(String status) {
        this.status = status;
        return this;
    }

    public ChaosExperiment presetName(String presetName) {
        this.presetName = presetName;
        return this;
    }

    public ChaosExperiment faults(List<FaultConfig> faults) {
        this.faults = faults;
        return this;
    }

    public ChaosExperiment target(TargetConfig target) {
        this.target = target;
        return this;
    }

    public ChaosExperiment steadyState(SteadyState steadyState) {
        this.steadyState = steadyState;
        return this;
    }

    public ChaosExperiment schedule(ScheduleConfig schedule) {
        this.schedule = schedule;
        return this;
    }

    public ChaosExperiment safety(SafetyConfig safety) {
        this.safety = safety;
        return this;
    }

    public ChaosExperiment results(ChaosResults results) {
        this.results = results;
        return this;
    }

    public ChaosExperiment durationSec(Integer durationSec) {
        this.durationSec = durationSec;
        return this;
    }

    public ChaosExperiment warmupSec(Integer warmupSec) {
        this.warmupSec = warmupSec;
        return this;
    }

    public ChaosExperiment cooldownSec(Integer cooldownSec) {
        this.cooldownSec = cooldownSec;
        return this;
    }

    public ChaosExperiment createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getPresetName() {
        return presetName;
    }

    public List<FaultConfig> getFaults() {
        return faults;
    }

    public TargetConfig getTarget() {
        return target;
    }

    public SteadyState getSteadyState() {
        return steadyState;
    }

    public ScheduleConfig getSchedule() {
        return schedule;
    }

    public SafetyConfig getSafety() {
        return safety;
    }

    public ChaosResults getResults() {
        return results;
    }

    public Integer getDurationSec() {
        return durationSec;
    }

    public Integer getWarmupSec() {
        return warmupSec;
    }

    public Integer getCooldownSec() {
        return cooldownSec;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getEndedAt() {
        return endedAt;
    }

    @Override
    public String toString() {
        return "ChaosExperiment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
