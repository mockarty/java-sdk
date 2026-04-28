// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single row in a merged test run response — either the parent (mode="merged")
 * or one of the attached source runs. Mirrors {@code ActiveTestRunRow} emitted
 * by {@code /api/v1/test-runs/merges}.
 *
 * <p>The backing Go struct carries no Jackson field aliases, so JSON uses
 * capitalised field names ({@code ID}, {@code Namespace}, …). Annotations
 * below pin the mapping so we don't rely on auto-detection.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergedTestRun {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("Namespace")
    private String namespace;

    @JsonProperty("NodeID")
    private String nodeId;

    @JsonProperty("RunType")
    private String runType;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Message")
    private String message;

    @JsonProperty("TaskID")
    private String taskId;

    @JsonProperty("MetaJSON")
    private String metaJson;

    @JsonProperty("Mode")
    private String mode;

    @JsonProperty("ReferenceID")
    private String referenceId;

    @JsonProperty("UserID")
    private String userId;

    @JsonProperty("Progress")
    private int progress;

    @JsonProperty("StartedAt")
    private String startedAt;

    @JsonProperty("UpdatedAt")
    private String updatedAt;

    @JsonProperty("CompletedAt")
    private String completedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getRunType() {
        return runType;
    }

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getMetaJson() {
        return metaJson;
    }

    public void setMetaJson(String metaJson) {
        this.metaJson = metaJson;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
