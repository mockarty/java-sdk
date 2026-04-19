// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Per-status item tallies inside a {@link UnifiedReport}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UnifiedReportCounts {

    @JsonProperty("total")
    private int total;

    @JsonProperty("passed")
    private int passed;

    @JsonProperty("failed")
    private int failed;

    @JsonProperty("skipped")
    private int skipped;

    @JsonProperty("broken")
    private int broken;

    public UnifiedReportCounts() {
    }

    public int getTotal() {
        return total;
    }

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getBroken() {
        return broken;
    }

    public UnifiedReportCounts setTotal(int total) {
        this.total = total;
        return this;
    }

    public UnifiedReportCounts setPassed(int passed) {
        this.passed = passed;
        return this;
    }

    public UnifiedReportCounts setFailed(int failed) {
        this.failed = failed;
        return this;
    }

    public UnifiedReportCounts setSkipped(int skipped) {
        this.skipped = skipped;
        return this;
    }

    public UnifiedReportCounts setBroken(int broken) {
        this.broken = broken;
        return this;
    }
}
