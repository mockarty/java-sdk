// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

package ru.mockarty.model;

/** Result of a unified mission start or idempotent OriginRef replay. */
public class MissionStartResponse {
    private UnifiedMission mission;
    private boolean created;

    public UnifiedMission getMission() { return mission; }
    public boolean isCreated() { return created; }
}
