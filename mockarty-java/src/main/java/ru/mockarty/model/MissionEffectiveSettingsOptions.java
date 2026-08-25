// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Optional selectors for the unified mission settings preview. */
public class MissionEffectiveSettingsOptions {
    private String productId;
    private String missionId;
    private Integer runWindowMinutes;

    public MissionEffectiveSettingsOptions productId(String value) { this.productId = value; return this; }
    public MissionEffectiveSettingsOptions missionId(String value) { this.missionId = value; return this; }
    public MissionEffectiveSettingsOptions runWindowMinutes(Integer value) { this.runWindowMinutes = value; return this; }

    public String getProductId() { return productId; }
    public String getMissionId() { return missionId; }
    public Integer getRunWindowMinutes() { return runWindowMinutes; }
}
