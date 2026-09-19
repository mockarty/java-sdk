// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file in the project root for full license details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Accounting for every mock considered, so a small document can be told apart
 * from a filter that matched nothing.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpecExportStats {

    @JsonProperty("considered")
    private Integer considered;

    @JsonProperty("exported")
    private Integer exported;

    /** Mocks whose protocol does not belong to the requested format. */
    @JsonProperty("skipped")
    private Integer skipped;

    @JsonProperty("paths")
    private Integer paths;

    @JsonProperty("operations")
    private Integer operations;

    @JsonProperty("channels")
    private Integer channels;

    public SpecExportStats() {
    }

    public Integer getConsidered() {
        return considered;
    }

    public void setConsidered(Integer considered) {
        this.considered = considered;
    }

    public Integer getExported() {
        return exported;
    }

    public void setExported(Integer exported) {
        this.exported = exported;
    }

    public Integer getSkipped() {
        return skipped;
    }

    public void setSkipped(Integer skipped) {
        this.skipped = skipped;
    }

    public Integer getPaths() {
        return paths;
    }

    public void setPaths(Integer paths) {
        this.paths = paths;
    }

    public Integer getOperations() {
        return operations;
    }

    public void setOperations(Integer operations) {
        this.operations = operations;
    }

    public Integer getChannels() {
        return channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    @Override
    public String toString() {
        return "SpecExportStats{considered=" + considered + ", exported=" + exported +
                ", skipped=" + skipped + '}';
    }
}
