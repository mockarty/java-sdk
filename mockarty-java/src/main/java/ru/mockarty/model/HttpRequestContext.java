// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP request context for matching HTTP mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpRequestContext {

    @JsonProperty("route")
    private String route;

    @JsonProperty("routePattern")
    private String routePattern;

    @JsonProperty("httpMethod")
    private String httpMethod;

    @JsonProperty("conditions")
    private List<Condition> conditions;

    @JsonProperty("queryParams")
    private List<Condition> queryParams;

    @JsonProperty("header")
    private List<Condition> header;

    @JsonProperty("sortArray")
    private Boolean sortArray;

    public HttpRequestContext() {
    }

    // Builder-style setters

    public HttpRequestContext route(String route) {
        this.route = route;
        return this;
    }

    public HttpRequestContext routePattern(String routePattern) {
        this.routePattern = routePattern;
        return this;
    }

    public HttpRequestContext httpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
        return this;
    }

    public HttpRequestContext conditions(List<Condition> conditions) {
        this.conditions = conditions;
        return this;
    }

    public HttpRequestContext addCondition(Condition condition) {
        if (this.conditions == null) {
            this.conditions = new ArrayList<>();
        }
        this.conditions.add(condition);
        return this;
    }

    public HttpRequestContext queryParams(List<Condition> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public HttpRequestContext addQueryParam(Condition condition) {
        if (this.queryParams == null) {
            this.queryParams = new ArrayList<>();
        }
        this.queryParams.add(condition);
        return this;
    }

    public HttpRequestContext header(List<Condition> header) {
        this.header = header;
        return this;
    }

    public HttpRequestContext addHeader(Condition condition) {
        if (this.header == null) {
            this.header = new ArrayList<>();
        }
        this.header.add(condition);
        return this;
    }

    public HttpRequestContext sortArray(boolean sortArray) {
        this.sortArray = sortArray;
        return this;
    }

    // Getters

    public String getRoute() {
        return route;
    }

    public String getRoutePattern() {
        return routePattern;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public List<Condition> getQueryParams() {
        return queryParams;
    }

    public List<Condition> getHeader() {
        return header;
    }

    public Boolean getSortArray() {
        return sortArray;
    }

    @Override
    public String toString() {
        return "HttpRequestContext{" +
                "route='" + route + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                '}';
    }
}
