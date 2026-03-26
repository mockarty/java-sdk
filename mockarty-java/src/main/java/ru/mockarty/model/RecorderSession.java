// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a traffic recording session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecorderSession {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("targetUrl")
    private String targetUrl;

    @JsonProperty("status")
    private String status;

    @JsonProperty("port")
    private Integer port;

    @JsonProperty("entryCount")
    private Integer entryCount;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("stoppedAt")
    private String stoppedAt;

    public RecorderSession() {
    }

    // Builder-style setters

    public RecorderSession id(String id) {
        this.id = id;
        return this;
    }

    public RecorderSession name(String name) {
        this.name = name;
        return this;
    }

    public RecorderSession namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public RecorderSession targetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        return this;
    }

    public RecorderSession status(String status) {
        this.status = status;
        return this;
    }

    public RecorderSession port(Integer port) {
        this.port = port;
        return this;
    }

    public RecorderSession entryCount(Integer entryCount) {
        this.entryCount = entryCount;
        return this;
    }

    public RecorderSession createdAt(String createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public RecorderSession stoppedAt(String stoppedAt) {
        this.stoppedAt = stoppedAt;
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

    public String getStatus() {
        return status;
    }

    public Integer getPort() {
        return port;
    }

    public Integer getEntryCount() {
        return entryCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getStoppedAt() {
        return stoppedAt;
    }

    @Override
    public String toString() {
        return "RecorderSession{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
