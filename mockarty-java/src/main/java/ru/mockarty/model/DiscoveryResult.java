// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for {@code POST /tcm/discovery}. Mirrors the Go server's
 * {@code SyncResult} struct — see
 * {@code internal/tcm/discovery/discovery.go}.
 *
 * <p>{@link #getCreated() created} / {@link #getUpdated() updated} /
 * {@link #getOrphaned() orphaned} partition the cases the sync touched;
 * {@link #getTotal() total} is the number of cases in the uploaded
 * manifest.</p>
 */
public class DiscoveryResult {

    @JsonProperty("source")
    private String source;

    @JsonProperty("created")
    private int created;

    @JsonProperty("updated")
    private int updated;

    @JsonProperty("orphaned")
    private int orphaned;

    @JsonProperty("total")
    private int total;

    public String getSource() { return source; }
    public int getCreated() { return created; }
    public int getUpdated() { return updated; }
    public int getOrphaned() { return orphaned; }
    public int getTotal() { return total; }

    public void setSource(String s) { this.source = s; }
    public void setCreated(int v) { this.created = v; }
    public void setUpdated(int v) { this.updated = v; }
    public void setOrphaned(int v) { this.orphaned = v; }
    public void setTotal(int v) { this.total = v; }

    @Override
    public String toString() {
        return "DiscoveryResult{source='" + source + "', created=" + created
                + ", updated=" + updated + ", orphaned=" + orphaned
                + ", total=" + total + '}';
    }
}
