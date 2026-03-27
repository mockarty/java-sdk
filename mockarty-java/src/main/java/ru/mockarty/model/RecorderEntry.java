// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * A single recorded request/response entry in a recording session.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RecorderEntry {

    @JsonProperty("id")
    private String id;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("method")
    private String method;

    @JsonProperty("path")
    private String path;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("requestHeaders")
    private Map<String, String> requestHeaders;

    @JsonProperty("requestBody")
    private Object requestBody;

    @JsonProperty("responseHeaders")
    private Map<String, String> responseHeaders;

    @JsonProperty("responseBody")
    private Object responseBody;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("timestamp")
    private String timestamp;

    public RecorderEntry() {
    }

    // Builder-style setters

    public RecorderEntry id(String id) {
        this.id = id;
        return this;
    }

    public RecorderEntry sessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public RecorderEntry method(String method) {
        this.method = method;
        return this;
    }

    public RecorderEntry path(String path) {
        this.path = path;
        return this;
    }

    public RecorderEntry statusCode(Integer statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public RecorderEntry requestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    public RecorderEntry requestBody(Object requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    public RecorderEntry responseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }

    public RecorderEntry responseBody(Object responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public RecorderEntry duration(Long duration) {
        this.duration = duration;
        return this;
    }

    public RecorderEntry timestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public Object getResponseBody() {
        return responseBody;
    }

    public Long getDuration() {
        return duration;
    }

    public String getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "RecorderEntry{" +
                "id='" + id + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", statusCode=" + statusCode +
                '}';
    }
}
