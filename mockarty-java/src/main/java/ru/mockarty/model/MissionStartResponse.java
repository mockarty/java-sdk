// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Result of a unified mission start or idempotent OriginRef replay. */
public class MissionStartResponse {
    private UnifiedMission mission;
    private boolean created;

    public UnifiedMission getMission() { return mission; }
    public boolean isCreated() { return created; }
}
