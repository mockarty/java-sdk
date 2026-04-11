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

    @JsonProperty("category")
    private String category;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("triagedStatus")
    private String triagedStatus;

    @JsonProperty("requestMethod")
    private String requestMethod;

    @JsonProperty("requestUrl")
    private String requestUrl;

    @JsonProperty("responseStatus")
    private Integer responseStatus;

    @JsonProperty("createdAt")
    private String createdAt;

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

    public FuzzingFinding category(String category) {
        this.category = category;
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

    public FuzzingFinding triagedStatus(String triagedStatus) {
        this.triagedStatus = triagedStatus;
        return this;
    }

    public FuzzingFinding requestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
        return this;
    }

    public FuzzingFinding requestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
        return this;
    }

    public FuzzingFinding responseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public String getCategory() {
        return category;
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

    public String getTriagedStatus() {
        return triagedStatus;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "FuzzingFinding{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", severity='" + severity + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
