// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a quarantine entry for suppressing known false-positive fuzzing findings.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuarantineEntry {

    @JsonProperty("id")
    private String id;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("fingerprint")
    private String fingerprint;

    @JsonProperty("category")
    private String category;

    @JsonProperty("endpointPattern")
    private String endpointPattern;

    @JsonProperty("title")
    private String title;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("sourceFindingId")
    private String sourceFindingId;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("createdAt")
    private String createdAt;

    public QuarantineEntry() {
    }

    // Builder-style setters

    public QuarantineEntry id(String id) {
        this.id = id;
        return this;
    }

    public QuarantineEntry namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public QuarantineEntry fingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
        return this;
    }

    public QuarantineEntry category(String category) {
        this.category = category;
        return this;
    }

    public QuarantineEntry endpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
        return this;
    }

    public QuarantineEntry title(String title) {
        this.title = title;
        return this;
    }

    public QuarantineEntry reason(String reason) {
        this.reason = reason;
        return this;
    }

    public QuarantineEntry sourceFindingId(String sourceFindingId) {
        this.sourceFindingId = sourceFindingId;
        return this;
    }

    public QuarantineEntry createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getCategory() {
        return category;
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public String getTitle() {
        return title;
    }

    public String getReason() {
        return reason;
    }

    public String getSourceFindingId() {
        return sourceFindingId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "QuarantineEntry{" +
                "id='" + id + '\'' +
                ", namespace='" + namespace + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                ", category='" + category + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
