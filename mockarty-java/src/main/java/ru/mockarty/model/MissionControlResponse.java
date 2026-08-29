// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import java.util.List;
import java.util.Map;

/** Unified mission plus the durable receipt returned by an operator control. */
public class MissionControlResponse {
    private Map<String, Object> error;
    private UnifiedMission mission;
    private MissionControlReceipt control;
    private List<MissionExecutionBinding> executionBindings;
    private boolean executionBindingsAvailable;
    private boolean pending;

    public Map<String, Object> getError() { return error; }
    public UnifiedMission getMission() { return mission; }
    public MissionControlReceipt getControl() { return control; }
    public List<MissionExecutionBinding> getExecutionBindings() {
        return executionBindings == null ? List.of() : executionBindings;
    }
    public boolean isExecutionBindingsAvailable() { return executionBindingsAvailable; }
    public boolean isPending() { return pending; }
}
