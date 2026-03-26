// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a scheduled performance test.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfSchedule {

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

    public PerfSchedule() {
    }

    // Builder-style setters

    public PerfSchedule id(String id) {
        this.id = id;
        return this;
    }

    public PerfSchedule configId(String configId) {
        this.configId = configId;
        return this;
    }

    public PerfSchedule cron(String cron) {
        this.cron = cron;
        return this;
    }

    public PerfSchedule enabled(Boolean enabled) {
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
        return "PerfSchedule{" +
                "id='" + id + '\'' +
                ", configId='" + configId + '\'' +
                ", cron='" + cron + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}
