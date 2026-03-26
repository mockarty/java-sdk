// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single contract violation found during validation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractViolation {

    @JsonProperty("path")
    private String path;

    @JsonProperty("message")
    private String message;

    @JsonProperty("severity")
    private String severity;

    @JsonProperty("expected")
    private Object expected;

    @JsonProperty("actual")
    private Object actual;

    @JsonProperty("rule")
    private String rule;

    public ContractViolation() {
    }

    // Builder-style setters

    public ContractViolation path(String path) {
        this.path = path;
        return this;
    }

    public ContractViolation message(String message) {
        this.message = message;
        return this;
    }

    public ContractViolation severity(String severity) {
        this.severity = severity;
        return this;
    }

    public ContractViolation expected(Object expected) {
        this.expected = expected;
        return this;
    }

    public ContractViolation actual(Object actual) {
        this.actual = actual;
        return this;
    }

    public ContractViolation rule(String rule) {
        this.rule = rule;
        return this;
    }

    // Getters

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public String getSeverity() {
        return severity;
    }

    public Object getExpected() {
        return expected;
    }

    public Object getActual() {
        return actual;
    }

    public String getRule() {
        return rule;
    }

    @Override
    public String toString() {
        return "ContractViolation{" +
                "path='" + path + '\'' +
                ", message='" + message + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}
