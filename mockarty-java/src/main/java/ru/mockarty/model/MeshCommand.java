// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One piece of work: bound to a device, or unbound with
 * {@link #getPlacementReason()} explaining why nothing could take it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshCommand {
    private String id;
    private String namespace;
    private String kind;
    private String state;
    private String targetDeviceId;
    private String leaseOwner;
    private String placementReason;
    private String resultSummary;
    private String resultRef;
    private String failureReason;
    private List<String> requiredCapabilities;
    private Map<String, Object> payload;
    private Instant queuedAt;
    private Instant claimedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private Instant leaseExpiresAt;

    public String getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getKind() { return kind; }
    public String getState() { return state; }
    public String getTargetDeviceId() { return targetDeviceId; }
    public String getLeaseOwner() { return leaseOwner; }
    public String getPlacementReason() { return placementReason; }
    public String getResultSummary() { return resultSummary; }
    public String getResultRef() { return resultRef; }
    public String getFailureReason() { return failureReason; }
    public List<String> getRequiredCapabilities() { return requiredCapabilities; }
    public Map<String, Object> getPayload() { return payload; }
    public Instant getQueuedAt() { return queuedAt; }
    public Instant getClaimedAt() { return claimedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
}
