// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomousMissionListResponse {
    private List<AutonomousMission> missions;
    private int total;

    public List<AutonomousMission> getMissions() { return missions == null ? List.of() : missions; }
    public int getTotal() { return total; }
}
