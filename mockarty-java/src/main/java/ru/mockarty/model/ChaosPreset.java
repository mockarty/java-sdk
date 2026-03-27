// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents metadata about a predefined chaos experiment preset.
 *
 * <p>Maps to the server-side PresetInfo struct in internal/chaos/presets.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChaosPreset {

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("faultTypes")
    private List<String> faultTypes;

    @JsonProperty("riskLevel")
    private String riskLevel;

    public ChaosPreset() {
    }

    // Builder-style setters

    public ChaosPreset name(String name) {
        this.name = name;
        return this;
    }

    public ChaosPreset displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ChaosPreset description(String description) {
        this.description = description;
        return this;
    }

    public ChaosPreset faultTypes(List<String> faultTypes) {
        this.faultTypes = faultTypes;
        return this;
    }

    public ChaosPreset riskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
        return this;
    }

    // Getters

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getFaultTypes() {
        return faultTypes;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    @Override
    public String toString() {
        return "ChaosPreset{" +
                "name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                '}';
    }
}
