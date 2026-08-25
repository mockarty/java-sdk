// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Stable mission projection returned by autonomous mission read endpoints. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutonomousMission {
    private String id;
    private String namespace;
    private String goal;
    private String status;
    private String autonomy;
    private String source;
    @JsonProperty("userId") private String userId;
    @JsonProperty("traceId") private String traceId;
    @JsonProperty("sourceRef") private String sourceRef;
    @JsonProperty("createdAt") private String createdAt;
    @JsonProperty("updatedAt") private String updatedAt;
    @JsonProperty("leaseExpiresAt") private String leaseExpiresAt;
    @JsonProperty("awaitingQuestion") private String awaitingQuestion;
    @JsonProperty("awaitingRequestId") private String awaitingRequestId;
    private String plan;
    @JsonProperty("leaseOwner") private String leaseOwner;
    @JsonProperty("contextRefs") private List<Map<String, String>> contextRefs;
    private List<String> options;
    private Map<String, Object> budget;
    @JsonProperty("spentTokens") private long spentTokens;
    @JsonProperty("stepCount") private int stepCount;
    @JsonProperty("stepInProgress") private boolean stepInProgress;

    public String getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getGoal() { return goal; }
    public String getStatus() { return status; }
    public String getAutonomy() { return autonomy; }
    public String getSource() { return source; }
    public String getUserId() { return userId; }
    public String getTraceId() { return traceId; }
    public String getSourceRef() { return sourceRef; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public String getLeaseExpiresAt() { return leaseExpiresAt; }
    public String getAwaitingQuestion() { return awaitingQuestion; }
    public String getAwaitingRequestId() { return awaitingRequestId; }
    public String getPlan() { return plan; }
    public String getLeaseOwner() { return leaseOwner; }
    public List<Map<String, String>> getContextRefs() { return contextRefs; }
    public List<String> getOptions() { return options; }
    public Map<String, Object> getBudget() { return budget; }
    public long getSpentTokens() { return spentTokens; }
    public int getStepCount() { return stepCount; }
    public boolean isStepInProgress() { return stepInProgress; }
}
