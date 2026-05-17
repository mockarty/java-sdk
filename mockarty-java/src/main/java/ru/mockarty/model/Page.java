// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Paginated response wrapper.
 *
 * <p>Wire shape varies by endpoint — Mockarty's admin uses
 * resource-specific keys instead of a uniform "items":
 * <ul>
 *   <li>{@code GET /api/v1/mocks} → {@code {"mocks":[...], "count":N, "limit":N}}</li>
 *   <li>{@code GET /api/v1/test-plans} → {@code {"items":[...], "count":N}}</li>
 *   <li>{@code GET /api/v1/tags} → {@code {"tags":[...], "namespace":"..."}}</li>
 * </ul>
 * The {@code @JsonAlias} annotations on {@code items} and {@code total}
 * make this class decode all of them transparently. Adding a new list
 * endpoint with a different collection key only requires adding one
 * more alias. Surfaced 2026-05-17 by the sibling-instance hunt that
 * caught the SaveMockResponse.isNew drift across all three SDKs.
 *
 * @param <T> the type of items in the page
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Page<T> {

    @JsonProperty("items")
    @JsonAlias({"mocks", "tags", "plans", "runs", "cases", "folders", "environments", "collections"})
    private List<T> items;

    @JsonProperty("total")
    @JsonAlias("count")
    private long total;

    @JsonProperty("offset")
    private int offset;

    @JsonProperty("limit")
    private int limit;

    public Page() {
    }

    public Page(List<T> items, long total, int offset, int limit) {
        this.items = items;
        this.total = total;
        this.offset = offset;
        this.limit = limit;
    }

    public static <T> Page<T> empty() {
        return new Page<>(Collections.emptyList(), 0, 0, 0);
    }

    public List<T> getItems() {
        return items != null ? items : Collections.emptyList();
    }

    public void setItems(List<T> items) {
        this.items = items;
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
        return "Page{" +
                "total=" + total +
                ", offset=" + offset +
                ", limit=" + limit +
                ", items=" + (items != null ? items.size() : 0) +
                '}';
    }
}
