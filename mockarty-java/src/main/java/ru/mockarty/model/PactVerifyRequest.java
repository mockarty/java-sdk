// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Request for pact provider verification, including message interaction support.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactVerifyRequest {

    @JsonProperty("pactId")
    private String pactId;

    @JsonProperty("pactContent")
    private String pactContent;

    @JsonProperty("providerBaseUrl")
    private String providerBaseUrl;

    @JsonProperty("providerStateUrl")
    private String providerStateUrl;

    @JsonProperty("messageCallbackUrl")
    private String messageCallbackUrl;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("timeout")
    private Integer timeout;

    public PactVerifyRequest() {
    }

    public PactVerifyRequest(String providerBaseUrl) {
        this.providerBaseUrl = providerBaseUrl;
    }

    public PactVerifyRequest pactId(String pactId) {
        this.pactId = pactId;
        return this;
    }

    public PactVerifyRequest pactContent(String pactContent) {
        this.pactContent = pactContent;
        return this;
    }

    public PactVerifyRequest providerBaseUrl(String providerBaseUrl) {
        this.providerBaseUrl = providerBaseUrl;
        return this;
    }

    public PactVerifyRequest providerStateUrl(String providerStateUrl) {
        this.providerStateUrl = providerStateUrl;
        return this;
    }

    public PactVerifyRequest messageCallbackUrl(String messageCallbackUrl) {
        this.messageCallbackUrl = messageCallbackUrl;
        return this;
    }

    public PactVerifyRequest headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    public PactVerifyRequest timeout(Integer timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getPactId() { return pactId; }
    public String getPactContent() { return pactContent; }
    public String getProviderBaseUrl() { return providerBaseUrl; }
    public String getProviderStateUrl() { return providerStateUrl; }
    public String getMessageCallbackUrl() { return messageCallbackUrl; }
    public Map<String, String> getHeaders() { return headers; }
    public Integer getTimeout() { return timeout; }
}
