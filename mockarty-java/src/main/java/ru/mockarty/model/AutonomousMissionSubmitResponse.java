// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomousMissionSubmitResponse {
    @JsonProperty("missionId") private String missionId;
    private String status;

    public String getMissionId() { return missionId; }
    public String getStatus() { return status; }
}
