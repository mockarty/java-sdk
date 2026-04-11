// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Paginated response for quarantine entries.
 * Uses "entries" as the collection key (distinct from the generic {@link Page} which uses "items").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FuzzingQuarantinePage {

    @JsonProperty("entries")
    private List<QuarantineEntry> entries;

    @JsonProperty("total")
    private long total;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("limit")
    private int limit;

    public FuzzingQuarantinePage() {
    }

    public List<QuarantineEntry> getEntries() {
        return entries != null ? entries : Collections.emptyList();
    }

    public void setEntries(List<QuarantineEntry> entries) {
        this.entries = entries;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public boolean hasMore() {
        return offset + limit < total;
    }

    @Override
    public String toString() {
        return "FuzzingQuarantinePage{" +
                "total=" + total +
                ", offset=" + offset +
                ", limit=" + limit +
                ", entries=" + (entries != null ? entries.size() : 0) +
                '}';
    }
}
