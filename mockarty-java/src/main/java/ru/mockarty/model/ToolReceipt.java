// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Durable state of one external action issued by an agent task. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToolReceipt {
    private String namespace;
    private String receiptKey;
    private String argsDigest;
    private String taskId;
    private String attemptId;
    private String toolName;
    private String effectClass;
    private String status;
    private String result;
    private String dispatchEventId;
    private String decision;
    private String decisionActor;
    private String decisionReason;
    private String decisionAt;
    private String createdAt;
    private String updatedAt;
    private long version;
    private int logicalOrdinal;
    private int replayCount;
    private int dispatchGeneration;
    @JsonProperty("isError")
    private boolean error;

    public String getNamespace() { return namespace; }
    public String getReceiptKey() { return receiptKey; }
    public String getArgsDigest() { return argsDigest; }
    public String getTaskId() { return taskId; }
    public String getAttemptId() { return attemptId; }
    public String getToolName() { return toolName; }
    public String getEffectClass() { return effectClass; }
    public String getStatus() { return status; }
    public String getResult() { return result; }
    public String getDispatchEventId() { return dispatchEventId; }
    public String getDecision() { return decision; }
    public String getDecisionActor() { return decisionActor; }
    public String getDecisionReason() { return decisionReason; }
    public String getDecisionAt() { return decisionAt; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public int getLogicalOrdinal() { return logicalOrdinal; }
    public int getReplayCount() { return replayCount; }
    public int getDispatchGeneration() { return dispatchGeneration; }
    public boolean isError() { return error; }
}
