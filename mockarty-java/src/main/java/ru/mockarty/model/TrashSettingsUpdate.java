// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Body accepted by the Recycle Bin settings PUT endpoints. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrashSettingsUpdate {

    @JsonProperty("retention_days")
    private int retentionDays;

    @JsonProperty("enabled")
    private boolean enabled;

    public TrashSettingsUpdate() {}

    public TrashSettingsUpdate(int retentionDays, boolean enabled) {
        this.retentionDays = retentionDays;
        this.enabled = enabled;
    }

    public TrashSettingsUpdate retentionDays(int value) { this.retentionDays = value; return this; }
    public TrashSettingsUpdate enabled(boolean value) { this.enabled = value; return this; }

    public int getRetentionDays() { return retentionDays; }
    public boolean isEnabled() { return enabled; }
}
