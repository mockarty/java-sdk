// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One test case in a discovery manifest.
 *
 * <p>Wire shape for an element of {@code Manifest.cases} on
 * {@code POST /api/v1/namespaces/:namespace/tcm/discovery}. Defined
 * server-side in {@code internal/tcm/discovery/discovery.go}
 * ({@code ManifestCase}).</p>
 *
 * <p>{@link #fullName(String) fullName} is REQUIRED — it is the
 * deterministic per-test identity the server upserts on (and the same key
 * a later {@code /tcm/external-runs} report uses, so a discovered test and
 * its execution result land on the same TCM case). The remaining fields
 * are optional and dropped from the wire payload when empty.</p>
 *
 * <p>Mirrors the Go ({@code DiscoveryManifestCase}) and Python SDK
 * surfaces 1:1 — do NOT rename JSON keys without coordinating across
 * SDKs.</p>
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DiscoveryManifestCase {

    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("suite")
    private String suite;

    @JsonProperty("description")
    private String description;

    @JsonProperty("sourceRef")
    private String sourceRef;

    @JsonProperty("labels")
    private List<String> labels;

    public DiscoveryManifestCase() {}

    /** Convenience constructor for the 80% case (fullName + display name). */
    public DiscoveryManifestCase(String fullName, String name) {
        this.fullName = fullName;
        this.name = name;
    }

    // ── Fluent setters ──────────────────────────────────────────────

    /** Deterministic per-test identity (e.g. {@code com.example.AuthTest#testLogin}). REQUIRED. */
    public DiscoveryManifestCase fullName(String v) { this.fullName = v; return this; }
    /** Human display name. Falls back to fullName server-side when empty. */
    public DiscoveryManifestCase name(String v) { this.name = v; return this; }
    /** Optional grouping hint (module / class). */
    public DiscoveryManifestCase suite(String v) { this.suite = v; return this; }
    /** Free-text description; set only on CREATE so manual edits survive re-syncs. */
    public DiscoveryManifestCase description(String v) { this.description = v; return this; }
    /** Where the test lives in code (file / repo URL) for "jump to source". */
    public DiscoveryManifestCase sourceRef(String v) { this.sourceRef = v; return this; }
    /** Tags applied to the case on CREATE. */
    public DiscoveryManifestCase labels(List<String> v) { this.labels = v; return this; }

    // ── Getters ─────────────────────────────────────────────────────

    public String getFullName() { return fullName; }
    public String getName() { return name; }
    public String getSuite() { return suite; }
    public String getDescription() { return description; }
    public String getSourceRef() { return sourceRef; }
    public List<String> getLabels() { return labels; }
}
