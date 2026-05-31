// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Wire shape for {@code POST /api/v1/namespaces/:namespace/tcm/discovery}.
 *
 * <p>A discovery manifest is the full inventory of test cases a framework
 * adapter knows about at collection time — including tests that did not
 * run. Syncing it keeps the TCM catalogue in lock-step with the source
 * tree: new tests are created, existing tests keep their human-authored
 * metadata, and tests absent from an authoritative manifest are marked
 * orphaned (never deleted).</p>
 *
 * <p>Defined server-side in {@code internal/tcm/discovery/discovery.go}
 * ({@code Manifest}). {@link #source(String) source} is REQUIRED — it is
 * the scope key, and pruning is scoped to it so one suite's manifest never
 * orphans another's cases.</p>
 *
 * <p>Mirrors the Go ({@code DiscoveryManifest}) and Python SDK surfaces
 * 1:1.</p>
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DiscoveryManifest {

    @JsonProperty("source")
    private String source;

    @JsonProperty("framework")
    private String framework;

    @JsonProperty("cases")
    private List<DiscoveryManifestCase> cases = new ArrayList<>();

    @JsonProperty("pruneMissing")
    private boolean pruneMissing;

    public DiscoveryManifest() {}

    /** Convenience constructor pinning the required scope key. */
    public DiscoveryManifest(String source) {
        this.source = source;
    }

    // ── Fluent setters ──────────────────────────────────────────────

    /** Scope key identifying this manifest's origin (e.g. {@code junit5:auth-suite}). REQUIRED. */
    public DiscoveryManifest source(String v) { this.source = v; return this; }
    /** Informational framework name (junit5 / pytest / ...). */
    public DiscoveryManifest framework(String v) { this.framework = v; return this; }
    /** Replaces the full case inventory. */
    public DiscoveryManifest cases(List<DiscoveryManifestCase> v) {
        this.cases = (v == null) ? new ArrayList<>() : v;
        return this;
    }
    /** Appends one case to the inventory. */
    public DiscoveryManifest addCase(DiscoveryManifestCase c) {
        if (c != null) {
            this.cases.add(c);
        }
        return this;
    }
    /** When true, cases discovered under this source but absent here are orphaned. */
    public DiscoveryManifest pruneMissing(boolean v) { this.pruneMissing = v; return this; }

    // ── Getters ─────────────────────────────────────────────────────

    public String getSource() { return source; }
    public String getFramework() { return framework; }
    public List<DiscoveryManifestCase> getCases() { return cases; }
    public boolean isPruneMissing() { return pruneMissing; }
}
