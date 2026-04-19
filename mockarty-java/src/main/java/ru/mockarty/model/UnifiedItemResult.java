// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * One result entry inside a {@link UnifiedReport}. Mirrors
 * {@code internal/testplan.AllureResult}. Unknown fields are ignored so
 * the SDK does not need to ship a release for every server-side schema
 * addition.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedItemResult {

    @JsonProperty("name")
    private String name;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("historyId")
    private String historyId;

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private String status;

    @JsonProperty("stage")
    private String stage;

    @JsonProperty("statusDetails")
    private Map<String, Object> statusDetails;

    @JsonProperty("labels")
    private List<Map<String, Object>> labels;

    @JsonProperty("parameters")
    private List<Map<String, Object>> parameters;

    @JsonProperty("attachments")
    private List<Map<String, Object>> attachments;

    @JsonProperty("start")
    private Long start;

    @JsonProperty("stop")
    private Long stop;

    public UnifiedItemResult() {
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHistoryId() {
        return historyId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getStage() {
        return stage;
    }

    public Map<String, Object> getStatusDetails() {
        return statusDetails;
    }

    public List<Map<String, Object>> getLabels() {
        return labels;
    }

    public List<Map<String, Object>> getParameters() {
        return parameters;
    }

    public List<Map<String, Object>> getAttachments() {
        return attachments;
    }

    public Long getStart() {
        return start;
    }

    public Long getStop() {
        return stop;
    }
}
