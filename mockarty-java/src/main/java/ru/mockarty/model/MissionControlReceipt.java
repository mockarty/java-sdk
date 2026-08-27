// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Public, restart-stable receipt for one unified mission control intent. */
public class MissionControlReceipt {
    private String createdAt;
    private String updatedAt;
    private String committedAt;
    private String resolvedAt;
    private String id;
    private String missionId;
    private String idempotencyKey;
    private String action;
    private String phase;
    private String outcome;
    private String reason;
    private String resolution;
    private String resolvedBy;
    private String resolutionReason;

    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getCommittedAt() { return committedAt; }
    public String getResolvedAt() { return resolvedAt; }
    public String getId() { return id; }
    public String getMissionId() { return missionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getAction() { return action; }
    public String getPhase() { return phase; }
    public String getOutcome() { return outcome; }
    public String getReason() { return reason; }
    public String getResolution() { return resolution; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolutionReason() { return resolutionReason; }
}
