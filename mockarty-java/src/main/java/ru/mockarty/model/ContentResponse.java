// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response configuration for a mock. Defines status code, headers, payload, and delay.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentResponse {

    @JsonProperty("headers")
    private Map<String, List<String>> headers;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("decode")
    private String decode;

    @JsonProperty("payload")
    private Object payload;

    @JsonProperty("payloadTemplatePath")
    private String payloadTemplatePath;

    @JsonProperty("error")
    private String error;

    @JsonProperty("errorDetails")
    private List<Map<String, Object>> errorDetails;

    @JsonProperty("delay")
    private Integer delay;

    @JsonProperty("sseEventChain")
    private Map<String, Object> sseEventChain;

    @JsonProperty("graphqlErrors")
    private List<Map<String, Object>> graphqlErrors;

    @JsonProperty("soapFault")
    private Map<String, Object> soapFault;

    @JsonProperty("mcpIsError")
    private Boolean mcpIsError;

    public ContentResponse() {
    }

    // Builder-style setters

    public ContentResponse headers(Map<String, List<String>> headers) {
        this.headers = headers;
        return this;
    }

    public ContentResponse statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public ContentResponse decode(String decode) {
        this.decode = decode;
        return this;
    }

    public ContentResponse payload(Object payload) {
        this.payload = payload;
        return this;
    }

    public ContentResponse payloadTemplatePath(String payloadTemplatePath) {
        this.payloadTemplatePath = payloadTemplatePath;
        return this;
    }

    public ContentResponse error(String error) {
        this.error = error;
        return this;
    }

    public ContentResponse errorDetails(List<Map<String, Object>> errorDetails) {
        this.errorDetails = errorDetails;
        return this;
    }

    public ContentResponse delay(int delay) {
        this.delay = delay;
        return this;
    }

    public ContentResponse sseEventChain(Map<String, Object> sseEventChain) {
        this.sseEventChain = sseEventChain;
        return this;
    }

    public ContentResponse graphqlErrors(List<Map<String, Object>> graphqlErrors) {
        this.graphqlErrors = graphqlErrors;
        return this;
    }

    public ContentResponse soapFault(Map<String, Object> soapFault) {
        this.soapFault = soapFault;
        return this;
    }

    public ContentResponse mcpIsError(boolean mcpIsError) {
        this.mcpIsError = mcpIsError;
        return this;
    }

    // Getters

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getDecode() {
        return decode;
    }

    public Object getPayload() {
        return payload;
    }

    public String getPayloadTemplatePath() {
        return payloadTemplatePath;
    }

    public String getError() {
        return error;
    }

    public List<Map<String, Object>> getErrorDetails() {
        return errorDetails;
    }

    public Integer getDelay() {
        return delay;
    }

    public Map<String, Object> getSseEventChain() {
        return sseEventChain;
    }

    public List<Map<String, Object>> getGraphqlErrors() {
        return graphqlErrors;
    }

    public Map<String, Object> getSoapFault() {
        return soapFault;
    }

    public Boolean getMcpIsError() {
        return mcpIsError;
    }

    @Override
    public String toString() {
        return "ContentResponse{" +
                "statusCode=" + statusCode +
                ", payload=" + payload +
                ", delay=" + delay +
                '}';
    }
}
