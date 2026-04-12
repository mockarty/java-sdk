// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for single JSON payload validation against a spec schema.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidatePayloadRequest {

    @JsonProperty("payload")
    private Object payload;

    @JsonProperty("specContent")
    private String specContent;

    @JsonProperty("contentType")
    private String contentType;

    @JsonProperty("specUrl")
    private String specUrl;

    @JsonProperty("endpoint")
    private String endpoint;

    @JsonProperty("statusCode")
    private String statusCode;

    public ValidatePayloadRequest() {
    }

    public ValidatePayloadRequest payload(Object payload) {
        this.payload = payload;
        return this;
    }

    public ValidatePayloadRequest specContent(String specContent) {
        this.specContent = specContent;
        return this;
    }

    public ValidatePayloadRequest contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    public ValidatePayloadRequest specUrl(String specUrl) {
        this.specUrl = specUrl;
        return this;
    }

    public ValidatePayloadRequest endpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public ValidatePayloadRequest statusCode(String statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Object getPayload() { return payload; }
    public String getSpecContent() { return specContent; }
    public String getContentType() { return contentType; }
    public String getSpecUrl() { return specUrl; }
    public String getEndpoint() { return endpoint; }
    public String getStatusCode() { return statusCode; }
}
