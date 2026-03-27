// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Result of a contract validation run.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContractValidationResult {

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("valid")
    private Boolean valid;

    @JsonProperty("violations")
    private List<ContractViolation> violations;

    @JsonProperty("checkedAt")
    private String checkedAt;

    @JsonProperty("duration")
    private Long duration;

    public ContractValidationResult() {
    }

    // Builder-style setters

    public ContractValidationResult contractId(String contractId) {
        this.contractId = contractId;
        return this;
    }

    public ContractValidationResult status(String status) {
        this.status = status;
        return this;
    }

    public ContractValidationResult valid(Boolean valid) {
        this.valid = valid;
        return this;
    }

    public ContractValidationResult violations(List<ContractViolation> violations) {
        this.violations = violations;
        return this;
    }

    public ContractValidationResult checkedAt(String checkedAt) {
        this.checkedAt = checkedAt;
        return this;
    }

    public ContractValidationResult duration(Long duration) {
        this.duration = duration;
        return this;
    }

    // Getters

    public String getContractId() {
        return contractId;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getValid() {
        return valid;
    }

    public List<ContractViolation> getViolations() {
        return violations;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public Long getDuration() {
        return duration;
    }

    /**
     * Returns true if the contract validation passed with no violations.
     */
    public boolean isPassed() {
        return Boolean.TRUE.equals(valid);
    }

    @Override
    public String toString() {
        return "ContractValidationResult{" +
                "contractId='" + contractId + '\'' +
                ", valid=" + valid +
                ", violations=" + (violations != null ? violations.size() : 0) +
                '}';
    }
}
