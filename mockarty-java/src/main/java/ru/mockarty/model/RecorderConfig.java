// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for a traffic recorder.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecorderConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("targetUrl")
    private String targetUrl;

    @JsonProperty("port")
    private Integer port;

    public RecorderConfig() {
    }

    // Builder-style setters

    public RecorderConfig id(String id) {
        this.id = id;
        return this;
    }

    public RecorderConfig name(String name) {
        this.name = name;
        return this;
    }

    public RecorderConfig targetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
        return this;
    }

    public RecorderConfig port(Integer port) {
        this.port = port;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "RecorderConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", targetUrl='" + targetUrl + '\'' +
                '}';
    }
}
