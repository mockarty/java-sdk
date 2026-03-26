// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP (Model Context Protocol) request context for matching MCP mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("header")
    private List<Condition> header;

    @JsonProperty("method")
    private String method;

    @JsonProperty("tool")
    private String tool;

    @JsonProperty("resource")
    private String resource;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public MCPRequestContext() {
    }

    // Builder-style setters

    public MCPRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public MCPRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public MCPRequestContext header(List<Condition> header) {
        this.header = header;
        return this;
    }

    public MCPRequestContext addHeader(Condition condition) {
        if (this.header == null) {
            this.header = new ArrayList<>();
        }
        this.header.add(condition);
        return this;
    }

    public MCPRequestContext method(String method) {
        this.method = method;
        return this;
    }

    public MCPRequestContext tool(String tool) {
        this.tool = tool;
        return this;
    }

    public MCPRequestContext resource(String resource) {
        this.resource = resource;
        return this;
    }

    public MCPRequestContext description(String description) {
        this.description = description;
        return this;
    }

    public MCPRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getHeader() {
        return header;
    }

    public String getMethod() {
        return method;
    }

    public String getTool() {
        return tool;
    }

    public String getResource() {
        return resource;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "MCPRequestContext{" +
                "tool='" + tool + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
