// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Filter options for the Recycle Bin list endpoints.
 *
 * <p>All fields are optional — zero / empty values are omitted from the
 * outgoing query string so server defaults apply (limit=50, offset=0).</p>
 */
public class TrashListOptions {

    private List<String> entityTypes = new ArrayList<>();
    private String search;
    private String closedBy;
    private String cascadeGroupId;
    private Instant fromTime;
    private Instant toTime;
    private int limit;
    private int offset;

    public TrashListOptions entityType(String type) {
        if (type != null && !type.isEmpty()) {
            this.entityTypes.add(type);
        }
        return this;
    }

    public TrashListOptions entityTypes(List<String> types) {
        if (types != null) {
            this.entityTypes = new ArrayList<>(types);
        }
        return this;
    }

    public TrashListOptions search(String value) { this.search = value; return this; }
    public TrashListOptions closedBy(String value) { this.closedBy = value; return this; }
    public TrashListOptions cascadeGroupId(String value) { this.cascadeGroupId = value; return this; }
    public TrashListOptions fromTime(Instant value) { this.fromTime = value; return this; }
    public TrashListOptions toTime(Instant value) { this.toTime = value; return this; }
    public TrashListOptions limit(int value) { this.limit = value; return this; }
    public TrashListOptions offset(int value) { this.offset = value; return this; }

    public List<String> getEntityTypes() { return entityTypes; }
    public String getSearch() { return search; }
    public String getClosedBy() { return closedBy; }
    public String getCascadeGroupId() { return cascadeGroupId; }
    public Instant getFromTime() { return fromTime; }
    public Instant getToTime() { return toTime; }
    public int getLimit() { return limit; }
    public int getOffset() { return offset; }
}
