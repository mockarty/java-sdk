// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * gRPC request context for matching gRPC mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrpcRequestContext {

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("meta")
    private List<Condition> meta;

    @JsonProperty("service")
    private String service;

    @JsonProperty("method")
    private String method;

    @JsonProperty("methodType")
    private String methodType;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public GrpcRequestContext() {
    }

    // Builder-style setters

    public GrpcRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public GrpcRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public GrpcRequestContext meta(List<Condition> meta) {
        this.meta = meta;
        return this;
    }

    public GrpcRequestContext addMeta(Condition condition) {
        if (this.meta == null) {
            this.meta = new ArrayList<>();
        }
        this.meta.add(condition);
        return this;
    }

    public GrpcRequestContext service(String service) {
        this.service = service;
        return this;
    }

    public GrpcRequestContext method(String method) {
        this.method = method;
        return this;
    }

    public GrpcRequestContext methodType(String methodType) {
        this.methodType = methodType;
        return this;
    }

    public GrpcRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getMeta() {
        return meta;
    }

    public String getService() {
        return service;
    }

    public String getMethod() {
        return method;
    }

    public String getMethodType() {
        return methodType;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "GrpcRequestContext{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                '}';
    }
}
