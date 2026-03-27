// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Result of a Pact verification run.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PactVerificationResult {

    @JsonProperty("id")
    private String id;

    @JsonProperty("pactId")
    private String pactId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("violations")
    private List<Map<String, Object>> violations;

    @JsonProperty("verifiedAt")
    private String verifiedAt;

    public PactVerificationResult() {
    }

    // Builder-style setters

    public PactVerificationResult id(String id) {
        this.id = id;
        return this;
    }

    public PactVerificationResult pactId(String pactId) {
        this.pactId = pactId;
        return this;
    }

    public PactVerificationResult status(String status) {
        this.status = status;
        return this;
    }

    public PactVerificationResult violations(List<Map<String, Object>> violations) {
        this.violations = violations;
        return this;
    }

    public PactVerificationResult verifiedAt(String verifiedAt) {
        this.verifiedAt = verifiedAt;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getPactId() {
        return pactId;
    }

    public String getStatus() {
        return status;
    }

    public List<Map<String, Object>> getViolations() {
        return violations;
    }

    public String getVerifiedAt() {
        return verifiedAt;
    }

    @Override
    public String toString() {
        return "PactVerificationResult{" +
                "id='" + id + '\'' +
                ", pactId='" + pactId + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
