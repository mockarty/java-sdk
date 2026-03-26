// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Health check response from Mockarty server.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("releaseId")
    private String releaseId;

    @JsonProperty("errors")
    private Map<String, String> errors;

    @JsonProperty("checks")
    private Map<String, Object> checks;

    @JsonProperty("output")
    private String output;

    public HealthResponse() {
    }

    /**
     * Returns true if the server is healthy (status is "pass").
     */
    public boolean isHealthy() {
        return "pass".equals(status);
    }

    // Getters and setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public Map<String, Object> getChecks() {
        return checks;
    }

    public void setChecks(Map<String, Object> checks) {
        this.checks = checks;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    @Override
    public String toString() {
        return "HealthResponse{" +
                "status='" + status + '\'' +
                ", releaseId='" + releaseId + '\'' +
                '}';
    }
}
