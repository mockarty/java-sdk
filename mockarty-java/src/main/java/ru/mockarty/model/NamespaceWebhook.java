// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Represents a namespace-level webhook configuration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NamespaceWebhook {

    @JsonProperty("id")
    private String id;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("url")
    private String url;

    @JsonProperty("method")
    private String method;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("enabled")
    private Boolean enabled;

    public NamespaceWebhook() {
    }

    // Builder-style setters

    public NamespaceWebhook id(String id) {
        this.id = id;
        return this;
    }

    public NamespaceWebhook operation(String operation) {
        this.operation = operation;
        return this;
    }

    public NamespaceWebhook url(String url) {
        this.url = url;
        return this;
    }

    public NamespaceWebhook method(String method) {
        this.method = method;
        return this;
    }

    public NamespaceWebhook headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public NamespaceWebhook enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "NamespaceWebhook{" +
                "id='" + id + '\'' +
                ", operation='" + operation + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
