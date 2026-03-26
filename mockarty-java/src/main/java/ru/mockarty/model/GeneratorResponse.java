// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from mock generation, indicating created mocks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratorResponse {

    @JsonProperty("created")
    private Integer created;

    @JsonProperty("updated")
    private Integer updated;

    @JsonProperty("skipped")
    private Integer skipped;

    @JsonProperty("mocks")
    private List<Mock> mocks;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("errors")
    private List<String> errors;

    public GeneratorResponse() {
    }

    // Builder-style setters

    public GeneratorResponse created(Integer created) {
        this.created = created;
        return this;
    }

    public GeneratorResponse updated(Integer updated) {
        this.updated = updated;
        return this;
    }

    public GeneratorResponse skipped(Integer skipped) {
        this.skipped = skipped;
        return this;
    }

    public GeneratorResponse mocks(List<Mock> mocks) {
        this.mocks = mocks;
        return this;
    }

    public GeneratorResponse warnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public GeneratorResponse errors(List<String> errors) {
        this.errors = errors;
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

    public List<Mock> getMocks() {
        return mocks;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "GeneratorResponse{" +
                "created=" + created +
                ", updated=" + updated +
                ", skipped=" + skipped +
                '}';
    }
}
