// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

/** Durable public cancellation evidence for one exact child execution. */
public class MissionExecutionBinding {
    private String createdAt;
    private String updatedAt;
    private String id;
    private String nodeId;
    private String externalId;
    private String kind;
    private String state;
    private long graphRevision;
    private long generation;
    private long cancelEpoch;
    private int deliveryCount;

    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getId() { return id; }
    public String getNodeId() { return nodeId; }
    public String getExternalId() { return externalId; }
    public String getKind() { return kind; }
    public String getState() { return state; }
    public long getGraphRevision() { return graphRevision; }
    public long getGeneration() { return generation; }
    public long getCancelEpoch() { return cancelEpoch; }
    public int getDeliveryCount() { return deliveryCount; }
}
