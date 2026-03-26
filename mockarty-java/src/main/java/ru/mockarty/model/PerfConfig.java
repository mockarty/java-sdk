// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration for a performance test.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfConfig {

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

    @JsonProperty("rps")
    private Integer rps;

    @JsonProperty("createdAt")
    private String createdAt;

    public PerfConfig() {
    }

    // Builder-style setters

    public PerfConfig id(String id) {
        this.id = id;
        return this;
    }

    public PerfConfig name(String name) {
        this.name = name;
        return this;
    }

    public PerfConfig namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public PerfConfig targetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        return this;
    }

    public PerfConfig method(String method) {
        this.method = method;
        return this;
    }

    public PerfConfig headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public PerfConfig body(Object body) {
        this.body = body;
        return this;
    }

    public PerfConfig duration(Integer duration) {
        this.duration = duration;
        return this;
    }

    public PerfConfig concurrency(Integer concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public PerfConfig rps(Integer rps) {
        this.rps = rps;
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

    public Integer getRps() {
        return rps;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "PerfConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
