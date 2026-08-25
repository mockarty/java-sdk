// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Authoritative layered-settings preview used to fence a unified mission start. */
public class MissionEffectiveSettings {
    private String namespace;
    @JsonProperty("productId") private String productId;
    @JsonProperty("missionId") private String missionId;
    @JsonProperty("settingsDigest") private String settingsDigest;
    private List<MissionEffectiveSetting> settings;
    private int count;

    public String getNamespace() { return namespace; }
    public String getProductId() { return productId; }
    public String getMissionId() { return missionId; }
    public String getSettingsDigest() { return settingsDigest; }
    public List<MissionEffectiveSetting> getSettings() { return settings == null ? List.of() : settings; }
    public int getCount() { return count; }
}
