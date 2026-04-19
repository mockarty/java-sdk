// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Envelope returned by the Recycle Bin list endpoints. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashListResult {

    @JsonProperty("items")
    private List<TrashItem> items;

    @JsonProperty("total")
    private int total;

    @JsonProperty("limit")
    private int limit;

    @JsonProperty("offset")
    private int offset;

    public List<TrashItem> getItems() { return items == null ? List.of() : items; }
    public int getTotal() { return total; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
