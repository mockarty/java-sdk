// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of a "can I deploy" check for Pact contract testing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CanIDeployResult {

    @JsonProperty("ok")
    private Boolean ok;

    @JsonProperty("reason")
    private String reason;

    public CanIDeployResult() {
    }

    // Builder-style setters

    public CanIDeployResult ok(Boolean ok) {
        this.ok = ok;
        return this;
    }

    public CanIDeployResult reason(String reason) {
        this.reason = reason;
        return this;
    }

    // Getters

    public Boolean getOk() {
        return ok;
    }

    public String getReason() {
        return reason;
    }

    /**
     * Returns true if deployment is safe.
     */
    public boolean isSafe() {
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public String toString() {
        return "CanIDeployResult{" +
                "ok=" + ok +
                ", reason='" + reason + '\'' +
                '}';
    }
}
