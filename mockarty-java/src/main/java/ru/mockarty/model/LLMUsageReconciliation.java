// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageReconciliation {
    private long reserved;
    private long settled;
    private long released;
    private long expired;
    private long missingUsageEvent;
    private long orphanUsageEvent;
    public long getReserved() { return reserved; }
    public long getSettled() { return settled; }
    public long getReleased() { return released; }
    public long getExpired() { return expired; }
    public long getMissingUsageEvent() { return missingUsageEvent; }
    public long getOrphanUsageEvent() { return orphanUsageEvent; }
}
