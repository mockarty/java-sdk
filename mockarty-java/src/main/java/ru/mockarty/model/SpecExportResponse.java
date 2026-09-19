// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * A built specification plus what it could not carry.
 *
 * <p>Read {@link #getWarnings()} before trusting the document: a mock whose
 * response is built by a script, rotates through oneOf variants, proxies a real
 * upstream or reads a template file has no place in a specification, and the
 * operation it belongs to stays in the document with a default response. An empty
 * warning list means every mock translated — {@link #getStats()} accounts for the
 * ones that did not belong to the requested format.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecExportResponse {

    /** The document itself (OpenAPI 3.0 or AsyncAPI 2.6, as JSON). */
    @JsonProperty("spec")
    private Map<String, Object> spec;

    @JsonProperty("format")
    private String format;

    @JsonProperty("warnings")
    private List<SpecExportWarning> warnings;

    @JsonProperty("stats")
    private SpecExportStats stats;

    public SpecExportResponse() {
    }

    public Map<String, Object> getSpec() {
        return spec;
    }

    public void setSpec(Map<String, Object> spec) {
        this.spec = spec;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public List<SpecExportWarning> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<SpecExportWarning> warnings) {
        this.warnings = warnings;
    }

    public SpecExportStats getStats() {
        return stats;
    }

    public void setStats(SpecExportStats stats) {
        this.stats = stats;
    }

    @Override
    public String toString() {
        return "SpecExportResponse{format='" + format + "', warnings=" +
                (warnings == null ? 0 : warnings.size()) + '}';
    }
}
