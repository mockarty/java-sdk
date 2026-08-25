// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomousMissionFlow {
    private Map<String, Object> source;
    private List<Map<String, Object>> steps;
    private List<Map<String, Object>> artifacts;
    private AutonomousMission mission;

    public Map<String, Object> getSource() { return source; }
    public List<Map<String, Object>> getSteps() { return steps == null ? List.of() : steps; }
    public List<Map<String, Object>> getArtifacts() { return artifacts == null ? List.of() : artifacts; }
    public AutonomousMission getMission() { return mission; }
}
