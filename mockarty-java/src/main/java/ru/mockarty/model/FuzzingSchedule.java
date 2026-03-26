// Copyright (c) 2024-2026 Mockarty. All rights reserved.
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

    @JsonProperty("configId")
    private String configId;

    @JsonProperty("cron")
    private String cron;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("createdAt")
    private String createdAt;

    public FuzzingSchedule() {
    }

    // Builder-style setters

    public FuzzingSchedule id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingSchedule configId(String configId) {
        this.configId = configId;
        return this;
    }

    public FuzzingSchedule cron(String cron) {
        this.cron = cron;
        return this;
    }

    public FuzzingSchedule enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getConfigId() {
        return configId;
    }

    public String getCron() {
        return cron;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "FuzzingSchedule{" +
                "id='" + id + '\'' +
                ", configId='" + configId + '\'' +
                ", cron='" + cron + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
