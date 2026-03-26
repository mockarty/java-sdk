// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * GraphQL request context for matching GraphQL mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GraphQLRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("header")
    private List<Condition> header;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("field")
    private String field;

    @JsonProperty("type")
    private String type;

    @JsonProperty("path")
    private String path;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public GraphQLRequestContext() {
    }

    // Builder-style setters

    public GraphQLRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public GraphQLRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public GraphQLRequestContext header(List<Condition> header) {
        this.header = header;
        return this;
    }

    public GraphQLRequestContext addHeader(Condition condition) {
        if (this.header == null) {
            this.header = new ArrayList<>();
        }
        this.header.add(condition);
        return this;
    }

    public GraphQLRequestContext operation(String operation) {
        this.operation = operation;
        return this;
    }

    public GraphQLRequestContext field(String field) {
        this.field = field;
        return this;
    }

    public GraphQLRequestContext type(String type) {
        this.type = type;
        return this;
    }

    public GraphQLRequestContext path(String path) {
        this.path = path;
        return this;
    }

    public GraphQLRequestContext sortArray(boolean sortArray) {
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

    public String getOperation() {
        return operation;
    }

    public String getField() {
        return field;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "GraphQLRequestContext{" +
                "operation='" + operation + '\'' +
                ", field='" + field + '\'' +
                '}';
    }
}
