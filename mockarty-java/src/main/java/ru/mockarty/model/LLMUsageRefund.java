// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

public class LLMUsageRefund {
    private String createdAt;
    private String id;
    private String originalEventId;
    private String refundEventId;
    private String actorId;
    private String namespace;
    private String reason;

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOriginalEventId() { return originalEventId; }
    public void setOriginalEventId(String originalEventId) { this.originalEventId = originalEventId; }
    public String getRefundEventId() { return refundEventId; }
    public void setRefundEventId(String refundEventId) { this.refundEventId = refundEventId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
