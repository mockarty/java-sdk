// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single finding from a fuzzing run.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingFinding {

    @JsonProperty("id")
    private String id;

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("triageStatus")
    private String triageStatus;

    public FuzzingFinding() {
    }

    // Builder-style setters

    public FuzzingFinding id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingFinding runId(String runId) {
        this.runId = runId;
        return this;
    }

    public FuzzingFinding type(String type) {
        this.type = type;
        return this;
    }

    public FuzzingFinding severity(String severity) {
        this.severity = severity;
        return this;
    }

    public FuzzingFinding title(String title) {
        this.title = title;
        return this;
    }

    public FuzzingFinding description(String description) {
        this.description = description;
        return this;
    }

    public FuzzingFinding triageStatus(String triageStatus) {
        this.triageStatus = triageStatus;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTriageStatus() {
        return triageStatus;
    }

    @Override
    public String toString() {
        return "FuzzingFinding{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", severity='" + severity + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
