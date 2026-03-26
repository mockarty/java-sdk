// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * SOAP request context for matching SOAP mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SoapRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("header")
    private List<Condition> header;

    @JsonProperty("service")
    private String service;

    @JsonProperty("method")
    private String method;

    @JsonProperty("action")
    private String action;

    @JsonProperty("path")
    private String path;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public SoapRequestContext() {
    }

    // Builder-style setters

    public SoapRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public SoapRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public SoapRequestContext header(List<Condition> header) {
        this.header = header;
        return this;
    }

    public SoapRequestContext addHeader(Condition condition) {
        if (this.header == null) {
            this.header = new ArrayList<>();
        }
        this.header.add(condition);
        return this;
    }

    public SoapRequestContext service(String service) {
        this.service = service;
        return this;
    }

    public SoapRequestContext method(String method) {
        this.method = method;
        return this;
    }

    public SoapRequestContext action(String action) {
        this.action = action;
        return this;
    }

    public SoapRequestContext path(String path) {
        this.path = path;
        return this;
    }

    public SoapRequestContext sortArray(boolean sortArray) {
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

    public String getService() {
        return service;
    }

    public String getMethod() {
        return method;
    }

    public String getAction() {
        return action;
    }

    public String getPath() {
        return path;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "SoapRequestContext{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
