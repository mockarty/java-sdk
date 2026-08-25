// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Configuration for a performance test.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfConfig {

    private static final Set<String> TYPED_JSON_NAMES = Set.of(
            "id", "name", "namespace", "script", "options", "collectionId", "parentId", "userId",
            "sortOrder", "isFolder", "environment", "targetUrl", "method", "headers", "body",
            "duration", "concurrency", "rps", "createdAt", "updatedAt");

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("script")
    private String script;

    @JsonProperty("options")
    private PerfOptions options;

    @JsonProperty("collectionId")
    private String collectionId;

    @JsonProperty("parentId")
    private String parentId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("sortOrder")
    private Integer sortOrder;

    @JsonProperty("isFolder")
    private Boolean isFolder;

    @JsonProperty("environment")
    private Map<String, Object> environment;

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

    @JsonProperty("updatedAt")
    private String updatedAt;

    @JsonIgnore
    private final Map<String, Object> extra = new LinkedHashMap<>();

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

    public PerfConfig script(String script) {
        this.script = script;
        return this;
    }

    public PerfConfig options(PerfOptions options) {
        this.options = options;
        return this;
    }

    public PerfConfig collectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public PerfConfig parentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public PerfConfig userId(String userId) {
        this.userId = userId;
        return this;
    }

    public PerfConfig sortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }

    public PerfConfig isFolder(Boolean isFolder) {
        this.isFolder = isFolder;
        return this;
    }

    public PerfConfig environment(Map<String, Object> environment) {
        this.environment = environment;
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

    public String getScript() {
        return script;
    }

    public PerfOptions getOptions() {
        return options;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getUserId() {
        return userId;
    }

    public int getSortOrder() {
        return sortOrder == null ? 0 : sortOrder;
    }

    public boolean getIsFolder() {
        return Boolean.TRUE.equals(isFolder);
    }

    public Map<String, Object> getEnvironment() {
        return environment;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    /** Retains newer server fields during a GET -> model -> PUT cycle. */
    @JsonAnySetter
    public void putExtra(String name, Object value) {
        if (!PerfJsonExtras.isReserved(TYPED_JSON_NAMES, name)) {
            extra.put(name, value);
        }
    }

    /** Emits retained newer-server fields without replacing typed fields. */
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return PerfJsonExtras.immutableCopy(extra);
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
