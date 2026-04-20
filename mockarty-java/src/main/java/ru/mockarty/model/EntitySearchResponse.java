// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Picker response envelope returned by the unified entity-search endpoint.
 *
 * <p>{@code total} is the count BEFORE pagination — useful for "showing N of
 * M" hints. {@link #getItems()} normalises a missing JSON {@code items}
 * field to an empty list so callers never need a null check.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntitySearchResponse {

    @JsonProperty("items")
    private List<EntitySearchResult> items;

    @JsonProperty("total")
    private int total;

    public List<EntitySearchResult> getItems() {
        return items == null ? Collections.emptyList() : items;
    }

    public int getTotal() { return total; }
}
