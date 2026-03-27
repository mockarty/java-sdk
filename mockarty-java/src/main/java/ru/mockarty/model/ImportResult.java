// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Result of an import operation (Postman, OpenAPI, HAR, etc.).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportResult {

    @JsonProperty("created")
    private Integer created;

    @JsonProperty("updated")
    private Integer updated;

    @JsonProperty("skipped")
    private Integer skipped;

    @JsonProperty("errors")
    private List<String> errors;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("mockIds")
    private List<String> mockIds;

    public ImportResult() {
    }

    // Builder-style setters

    public ImportResult created(Integer created) {
        this.created = created;
        return this;
    }

    public ImportResult updated(Integer updated) {
        this.updated = updated;
        return this;
    }

    public ImportResult skipped(Integer skipped) {
        this.skipped = skipped;
        return this;
    }

    public ImportResult errors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    public ImportResult warnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public ImportResult mockIds(List<String> mockIds) {
        this.mockIds = mockIds;
        return this;
    }

    // Getters

    public Integer getCreated() {
        return created;
    }

    public Integer getUpdated() {
        return updated;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getMockIds() {
        return mockIds;
    }

    @Override
    public String toString() {
        return "ImportResult{" +
                "created=" + created +
                ", updated=" + updated +
                ", skipped=" + skipped +
                '}';
    }
}
