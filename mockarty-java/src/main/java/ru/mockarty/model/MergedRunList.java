// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated list envelope returned by {@code GET /api/v1/test-runs/merges}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergedRunList {

    private List<MergedRunView> items = new ArrayList<>();
    private int total;
    private int limit;
    private int offset;

    public List<MergedRunView> getItems() {
        return items;
    }

    public void setItems(List<MergedRunView> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
