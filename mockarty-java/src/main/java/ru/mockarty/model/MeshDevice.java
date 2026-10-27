// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * One machine, phone or runner in the device mesh.
 *
 * <p>Presence is derived from the heartbeat, so {@link #getState()} reads
 * {@code offline} for a device that stopped reporting — nothing was deleted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshDevice {
    private String deviceId;
    private String namespace;
    private String kind;
    private String state;
    private String ownerUserId;
    private List<String> capabilities;
    private Map<String, String> labels;
    private Instant heartbeatAt;
    private Instant updatedAt;
    private Instant createdAt;

    public String getDeviceId() { return deviceId; }
    public String getNamespace() { return namespace; }
    public String getKind() { return kind; }
    public String getState() { return state; }
    public String getOwnerUserId() { return ownerUserId; }
    public List<String> getCapabilities() { return capabilities; }
    public Map<String, String> getLabels() { return labels; }
    public Instant getHeartbeatAt() { return heartbeatAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
