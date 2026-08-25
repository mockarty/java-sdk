// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One resolved autonomy setting and the layer that supplied it. */
public class MissionEffectiveSetting {
    private String key;
    private String value;
    private String layer;
    private String builtin;
    private boolean frozen;
    @JsonProperty("runtimeApplied") private boolean runtimeApplied;

    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getLayer() { return layer; }
    public String getBuiltin() { return builtin; }
    public boolean isFrozen() { return frozen; }
    public boolean isRuntimeApplied() { return runtimeApplied; }
}
