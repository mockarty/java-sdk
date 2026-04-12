// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Request for drift detection between mocks and live service.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DriftDetectionRequest {

    @JsonProperty("baseUrl")
    private String baseUrl;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("mockIds")
    private List<String> mockIds;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("timeout")
    private Integer timeout;

    public DriftDetectionRequest() {
    }

    public DriftDetectionRequest(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public DriftDetectionRequest baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public DriftDetectionRequest headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public DriftDetectionRequest namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public DriftDetectionRequest mockIds(List<String> mockIds) {
        this.mockIds = mockIds;
        return this;
    }

    public DriftDetectionRequest tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public DriftDetectionRequest timeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getBaseUrl() { return baseUrl; }
    public Map<String, String> getHeaders() { return headers; }
    public String getNamespace() { return namespace; }
    public List<String> getMockIds() { return mockIds; }
    public List<String> getTags() { return tags; }
    public Integer getTimeout() { return timeout; }
}
