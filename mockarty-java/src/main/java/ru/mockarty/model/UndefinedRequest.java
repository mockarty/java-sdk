// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an undefined (unmatched) request captured by Mockarty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UndefinedRequest {

    @JsonProperty("id")
    private String id;

    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("protocol")
    private String protocol;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("count")
    private Integer count;

    public UndefinedRequest() {
    }

    // Builder-style setters

    public UndefinedRequest id(String id) {
        this.id = id;
        return this;
    }

    public UndefinedRequest method(String method) {
        this.method = method;
        return this;
    }

    public UndefinedRequest path(String path) {
        this.path = path;
        return this;
    }

    public UndefinedRequest protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public UndefinedRequest timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public UndefinedRequest count(Integer count) {
        this.count = count;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Integer getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "UndefinedRequest{" +
                "id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", count=" + count +
                '}';
    }
}
