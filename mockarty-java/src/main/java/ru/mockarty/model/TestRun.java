// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Represents a test collection run with results.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestRun {

    @JsonProperty("id")
    private String id;

    @JsonProperty("collectionId")
    private String collectionId;

    /** Execution surface: "functional" (default), "load", "fuzz", "chaos", "contract". Added by migration 033. */
    @JsonProperty("mode")
    private String mode;

    /** UUID of the owning subsystem row (fuzz_configs.id, chaos_experiments.id, contract_registry.id). */
    @JsonProperty("referenceId")
    private String referenceId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("totalTests")
    private Integer totalTests;

    @JsonProperty("passedTests")
    private Integer passedTests;

    @JsonProperty("failedTests")
    private Integer failedTests;

    @JsonProperty("skippedTests")
    private Integer skippedTests;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("results")
    private List<Map<String, Object>> results;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("triggeredBy")
    private String triggeredBy;

    public TestRun() {
    }

    // Builder-style setters

    public TestRun id(String id) {
        this.id = id;
        return this;
    }

    public TestRun collectionId(String collectionId) {
        this.collectionId = collectionId;
        return this;
    }

    public TestRun mode(String mode) {
        this.mode = mode;
        return this;
    }

    public TestRun referenceId(String referenceId) {
        this.referenceId = referenceId;
        return this;
    }

    public TestRun status(String status) {
        this.status = status;
        return this;
    }

    public TestRun namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public TestRun totalTests(Integer totalTests) {
        this.totalTests = totalTests;
        return this;
    }

    public TestRun passedTests(Integer passedTests) {
        this.passedTests = passedTests;
        return this;
    }

    public TestRun failedTests(Integer failedTests) {
        this.failedTests = failedTests;
        return this;
    }

    public TestRun skippedTests(Integer skippedTests) {
        this.skippedTests = skippedTests;
        return this;
    }

    public TestRun duration(Long duration) {
        this.duration = duration;
        return this;
    }

    public TestRun results(List<Map<String, Object>> results) {
        this.results = results;
        return this;
    }

    public TestRun startedAt(String startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public TestRun finishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public TestRun triggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public String getMode() {
        return mode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getStatus() {
        return status;
    }

    public String getNamespace() {
        return namespace;
    }

    public Integer getTotalTests() {
        return totalTests;
    }

    public Integer getPassedTests() {
        return passedTests;
    }

    public Integer getFailedTests() {
        return failedTests;
    }

    public Integer getSkippedTests() {
        return skippedTests;
    }

    public Long getDuration() {
        return duration;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    @Override
    public String toString() {
        return "TestRun{" +
                "id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", totalTests=" + totalTests +
                ", passedTests=" + passedTests +
                ", failedTests=" + failedTests +
                '}';
    }
}
