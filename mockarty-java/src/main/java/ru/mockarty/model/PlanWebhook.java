// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * CI integration target for a Test Plan.
 *
 * <p>{@code secret} is write-only — the server stores a bcrypt hash and
 * never returns the plaintext again. Read-only lookups will only expose
 * {@code secretHash}.</p>
 *
 * <p>Named {@code PlanWebhook} to avoid clashing with
 * {@code NamespaceWebhook}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanWebhook {

    @JsonProperty("id")
    private String id;

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("url")
    private String url;

    /** Write-only. */
    @JsonProperty("secret")
    private String secret;

    @JsonProperty("secretHash")
    private String secretHash;

    @JsonProperty("events")
    private List<String> events;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("timeoutMs")
    private Long timeoutMs;

    @JsonProperty("retryCount")
    private Integer retryCount;

    @JsonProperty("retryBackoffMs")
    private Long retryBackoffMs;

    @JsonProperty("enabled")
    private Boolean enabled;

    @JsonProperty("lastCalledAt")
    private String lastCalledAt;

    @JsonProperty("lastStatus")
    private Integer lastStatus;

    public PlanWebhook() {
    }

    public PlanWebhook url(String url) {
        this.url = url;
        return this;
    }

    public PlanWebhook secret(String secret) {
        this.secret = secret;
        return this;
    }

    public PlanWebhook events(List<String> events) {
        this.events = events;
        return this;
    }

    public PlanWebhook headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public PlanWebhook timeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
        return this;
    }

    public PlanWebhook retryCount(Integer retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public PlanWebhook retryBackoffMs(Long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
        return this;
    }

    public PlanWebhook enabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getId() {
        return id;
    }

    public String getPlanId() {
        return planId;
    }

    public String getUrl() {
        return url;
    }

    public String getSecret() {
        return secret;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public List<String> getEvents() {
        return events;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public Long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public String getLastCalledAt() {
        return lastCalledAt;
    }

    public Integer getLastStatus() {
        return lastStatus;
    }
}
