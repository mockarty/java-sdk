// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Preview result from mock generation, showing what mocks would be created.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeneratorPreview {

    @JsonProperty("mocks")
    private List<Mock> mocks;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("warnings")
    private List<String> warnings;

    @JsonProperty("errors")
    private List<String> errors;

    public GeneratorPreview() {
    }

    // Builder-style setters

    public GeneratorPreview mocks(List<Mock> mocks) {
        this.mocks = mocks;
        return this;
    }

    public GeneratorPreview count(Integer count) {
        this.count = count;
        return this;
    }

    public GeneratorPreview warnings(List<String> warnings) {
        this.warnings = warnings;
        return this;
    }

    public GeneratorPreview errors(List<String> errors) {
        this.errors = errors;
        return this;
    }

    // Getters

    public List<Mock> getMocks() {
        return mocks;
    }

    public Integer getCount() {
        return count;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "GeneratorPreview{" +
                "count=" + count +
                ", warnings=" + warnings +
                '}';
    }
}
