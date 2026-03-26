// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Configuration for a fuzzing session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("targetUrl")
    private String targetUrl;

    @JsonProperty("method")
    private String method;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("body")
    private Object body;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("concurrency")
    private Integer concurrency;

    @JsonProperty("maxRequests")
    private Integer maxRequests;

    @JsonProperty("fuzzFields")
    private List<String> fuzzFields;

    @JsonProperty("securityChecks")
    private Boolean securityChecks;

    @JsonProperty("mutationTypes")
    private List<String> mutationTypes;

    public FuzzingConfig() {
    }

    // Builder-style setters

    public FuzzingConfig id(String id) {
        this.id = id;
        return this;
    }

    public FuzzingConfig name(String name) {
        this.name = name;
        return this;
    }

    public FuzzingConfig namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public FuzzingConfig targetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        return this;
    }

    public FuzzingConfig method(String method) {
        this.method = method;
        return this;
    }

    public FuzzingConfig headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public FuzzingConfig body(Object body) {
        this.body = body;
        return this;
    }

    public FuzzingConfig duration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public FuzzingConfig concurrency(Integer concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public FuzzingConfig maxRequests(Integer maxRequests) {
        this.maxRequests = maxRequests;
        return this;
    }

    public FuzzingConfig fuzzFields(List<String> fuzzFields) {
        this.fuzzFields = fuzzFields;
        return this;
    }

    public FuzzingConfig securityChecks(Boolean securityChecks) {
        this.securityChecks = securityChecks;
        return this;
    }

    public FuzzingConfig mutationTypes(List<String> mutationTypes) {
        this.mutationTypes = mutationTypes;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getBody() {
        return body;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public Integer getMaxRequests() {
        return maxRequests;
    }

    public List<String> getFuzzFields() {
        return fuzzFields;
    }

    public Boolean getSecurityChecks() {
        return securityChecks;
    }

    public List<String> getMutationTypes() {
        return mutationTypes;
    }

    @Override
    public String toString() {
        return "FuzzingConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
