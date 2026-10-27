// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/** One owner's decision to lend a device for a set of capabilities. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshGrant {
    private String deviceId;
    private String namespace;
    private String granteeUserId;
    private List<String> capabilities;
    private Instant createdAt;

    public String getDeviceId() { return deviceId; }
    public String getNamespace() { return namespace; }
    public String getGranteeUserId() { return granteeUserId; }
    public List<String> getCapabilities() { return capabilities; }
    public Instant getCreatedAt() { return createdAt; }
}
