// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an active or completed fuzzing run.
 *
 * <p>Wire shape on {@code POST /api/v1/fuzzing/run} is the three-field
 * envelope {@code {"taskId": "...", "resultId": "...", "runnerId": "..."}}.
 * The model uses Jackson aliases so unmarshalling works whether the
 * server emits the start-envelope OR the fuller progress envelope
 * (with id/configId/findings/etc.) read by ``getResult``.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingRun {

    /**
     * The user-visible run id (= ``fuzz_results.id``). On the start
     * response this is delivered as ``resultId`` — Jackson treats it
     * as an alias for ``id``.
     */
    @JsonProperty("id")
    @JsonAlias("resultId")
    private String id;

    /** The runner_tasks.id this run is attached to. Use with the Tasks API. */
    @JsonProperty("taskId")
    private String taskId;

    /** Assigned runner UUID; empty when the coordinator auto-picks. */
    @JsonProperty("runnerId")
    private String runnerId;

    @JsonProperty("configId")
    private String configId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("totalRequests")
    private Integer totalRequests;

    @JsonProperty("findings")
    private Integer findings;

    @JsonProperty("namespace")
    private String namespace;

    public FuzzingRun() {
    }

    // Builder-style setters

    public FuzzingRun id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingRun taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    public FuzzingRun runnerId(String runnerId) {
        this.runnerId = runnerId;
        return this;
    }

    public FuzzingRun configId(String configId) {
        this.configId = configId;
        return this;
    }

    public FuzzingRun status(String status) {
        this.status = status;
        return this;
    }

    public FuzzingRun startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public FuzzingRun finishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public FuzzingRun totalRequests(Integer totalRequests) {
        this.totalRequests = totalRequests;
        return this;
    }

    public FuzzingRun findings(Integer findings) {
        this.findings = findings;
        return this;
    }

    public FuzzingRun namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRunnerId() {
        return runnerId;
    }

    public String getConfigId() {
        return configId;
    }

    public String getStatus() {
        return status;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public Integer getTotalRequests() {
        return totalRequests;
    }

    public Integer getFindings() {
        return findings;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return "FuzzingRun{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", findings=" + findings +
                '}';
    }
}
