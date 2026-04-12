// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request for backward-compatibility checking between two spec versions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckCompatibilityRequest {

    @JsonProperty("oldSpecContent")
    private String oldSpecContent;

    @JsonProperty("oldContentType")
    private String oldContentType;

    @JsonProperty("newSpecContent")
    private String newSpecContent;

    @JsonProperty("newContentType")
    private String newContentType;

    public CheckCompatibilityRequest() {
    }

    public CheckCompatibilityRequest(String oldSpecContent, String newSpecContent) {
        this.oldSpecContent = oldSpecContent;
        this.newSpecContent = newSpecContent;
    }

    public CheckCompatibilityRequest oldSpecContent(String oldSpecContent) {
        this.oldSpecContent = oldSpecContent;
        return this;
    }

    public CheckCompatibilityRequest oldContentType(String oldContentType) {
        this.oldContentType = oldContentType;
        return this;
    }

    public CheckCompatibilityRequest newSpecContent(String newSpecContent) {
        this.newSpecContent = newSpecContent;
        return this;
    }

    public CheckCompatibilityRequest newContentType(String newContentType) {
        this.newContentType = newContentType;
        return this;
    }

    public String getOldSpecContent() { return oldSpecContent; }
    public String getOldContentType() { return oldContentType; }
    public String getNewSpecContent() { return newSpecContent; }
    public String getNewContentType() { return newContentType; }
}
