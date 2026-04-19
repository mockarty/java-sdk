// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Retention settings for a namespace or the global default. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashSettings {

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("retention_days")
    private int retentionDays;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("inherited")
    private Boolean inherited;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("updated_by")
    private String updatedBy;

    public TrashSettings() {}

    public TrashSettings retentionDays(int value) { this.retentionDays = value; return this; }
    public TrashSettings enabled(boolean value) { this.enabled = value; return this; }

    public String getScope() { return scope; }
    public String getNamespace() { return namespace; }
    public int getRetentionDays() { return retentionDays; }
    public boolean isEnabled() { return enabled; }
    public Boolean getInherited() { return inherited; }
    public String getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
}
