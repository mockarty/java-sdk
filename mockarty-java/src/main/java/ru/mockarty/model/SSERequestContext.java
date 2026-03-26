// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE (Server-Sent Events) request context for matching SSE mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SSERequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("headerConditions")
    private List<Condition> headerConditions;

    @JsonProperty("eventPath")
    private String eventPath;

    @JsonProperty("eventName")
    private String eventName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public SSERequestContext() {
    }

    // Builder-style setters

    public SSERequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public SSERequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public SSERequestContext headerConditions(List<Condition> headerConditions) {
        this.headerConditions = headerConditions;
        return this;
    }

    public SSERequestContext addHeaderCondition(Condition condition) {
        if (this.headerConditions == null) {
            this.headerConditions = new ArrayList<>();
        }
        this.headerConditions.add(condition);
        return this;
    }

    public SSERequestContext eventPath(String eventPath) {
        this.eventPath = eventPath;
        return this;
    }

    public SSERequestContext eventName(String eventName) {
        this.eventName = eventName;
        return this;
    }

    public SSERequestContext description(String description) {
        this.description = description;
        return this;
    }

    public SSERequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getHeaderConditions() {
        return headerConditions;
    }

    public String getEventPath() {
        return eventPath;
    }

    public String getEventName() {
        return eventName;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "SSERequestContext{" +
                "eventPath='" + eventPath + '\'' +
                ", eventName='" + eventName + '\'' +
                '}';
    }
}
