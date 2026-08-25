// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Mission projection returned by the unified mission ledger. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnifiedMission {
    @JsonProperty("createdAt") private Instant createdAt;
    @JsonProperty("updatedAt") private Instant updatedAt;
    @JsonProperty("closedAt") private Instant closedAt;
    private Map<String, Object> data;
    private String id;
    private String namespace;
    @JsonProperty("productId") private String productId;
    private String subject;
    private String kind;
    private String goal;
    private String autonomy;
    @JsonProperty("createdBy") private String createdBy;
    @JsonProperty("closedBy") private String closedBy;
    @JsonProperty("closedReason") private String closedReason;
    private String origin;
    @JsonProperty("originRef") private String originRef;
    private String status;
    private List<Map<String, Object>> chain;
    @JsonProperty("budgetTokensTotal") private long budgetTokensTotal;
    @JsonProperty("budgetTokensPerDay") private long budgetTokensPerDay;
    @JsonProperty("spentTokens") private long spentTokens;
    @JsonProperty("budgetUsdCap") private double budgetUsdCap;
    @JsonProperty("stepCount") private int stepCount;

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public Map<String, Object> getData() { return data; }
    public String getId() { return id; }
    public String getNamespace() { return namespace; }
    public String getProductId() { return productId; }
    public String getSubject() { return subject; }
    public String getKind() { return kind; }
    public String getGoal() { return goal; }
    public String getAutonomy() { return autonomy; }
    public String getCreatedBy() { return createdBy; }
    public String getClosedBy() { return closedBy; }
    public String getClosedReason() { return closedReason; }
    public String getOrigin() { return origin; }
    public String getOriginRef() { return originRef; }
    public String getStatus() { return status; }
    public List<Map<String, Object>> getChain() { return chain == null ? List.of() : chain; }
    public long getBudgetTokensTotal() { return budgetTokensTotal; }
    public long getBudgetTokensPerDay() { return budgetTokensPerDay; }
    public long getSpentTokens() { return spentTokens; }
    public double getBudgetUsdCap() { return budgetUsdCap; }
    public int getStepCount() { return stepCount; }
}
