// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a running performance test task.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfTask {

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("configId")
    private String configId;

    public PerfTask() {
    }

    // Builder-style setters

    public PerfTask taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public PerfTask status(String status) {
        this.status = status;
        return this;
    }

    public PerfTask configId(String configId) {
        this.configId = configId;
        return this;
    }

    // Getters

    public String getTaskId() {
        return taskId;
    }

    public String getStatus() {
        return status;
    }

    public String getConfigId() {
        return configId;
    }

    @Override
    public String toString() {
        return "PerfTask{" +
                "taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
