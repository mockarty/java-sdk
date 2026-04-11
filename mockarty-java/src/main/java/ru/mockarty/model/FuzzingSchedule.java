// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a scheduled fuzzing job.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingSchedule {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("configId")
    private String configId;

    @JsonProperty("cronExpression")
    private String cronExpression;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("notifyOnFailure")
    private Boolean notifyOnFailure;

    @JsonProperty("nextRunAt")
    private String nextRunAt;

    @JsonProperty("lastRunAt")
    private String lastRunAt;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("updatedAt")
    private String updatedAt;

    public FuzzingSchedule() {
    }

    // Builder-style setters

    public FuzzingSchedule id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingSchedule name(String name) {
        this.name = name;
        return this;
    }

    public FuzzingSchedule namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public FuzzingSchedule configId(String configId) {
        this.configId = configId;
        return this;
    }

    public FuzzingSchedule cronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
        return this;
    }

    public FuzzingSchedule enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public FuzzingSchedule notifyOnFailure(Boolean notifyOnFailure) {
        this.notifyOnFailure = notifyOnFailure;
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

    public String getConfigId() {
        return configId;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Boolean getNotifyOnFailure() {
        return notifyOnFailure;
    }

    public String getNextRunAt() {
        return nextRunAt;
    }

    public String getLastRunAt() {
        return lastRunAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "FuzzingSchedule{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", configId='" + configId + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
