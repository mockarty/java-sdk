// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Goal-first request accepted by POST /api/v1/missions; kind/chain are compatibility overrides. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class MissionStartRequest {
    private Map<String, Object> data;
    private String namespace;
    @JsonProperty("productId") private String productId;
    private String subject;
    private String kind;
    private String goal;
    private String autonomy;
    @JsonProperty("originRef") private String originRef;
    @JsonProperty("expectedSettingsDigest") private String expectedSettingsDigest;
    private List<MissionRevisionReference> targets;
    private List<MissionRevisionReference> artifacts;
    private List<String> chain;
    @JsonProperty("budgetTokensTotal") private long budgetTokensTotal;
    @JsonProperty("budgetTokensPerDay") private long budgetTokensPerDay;
    @JsonProperty("budgetUsdCap") private double budgetUsdCap;

    public MissionStartRequest data(Map<String, Object> value) { this.data = value; return this; }
    public MissionStartRequest namespace(String value) { this.namespace = value; return this; }
    public MissionStartRequest productId(String value) { this.productId = value; return this; }
    public MissionStartRequest subject(String value) { this.subject = value; return this; }
    public MissionStartRequest kind(String value) { this.kind = value; return this; }
    public MissionStartRequest goal(String value) { this.goal = value == null ? null : value.trim(); return this; }
    public MissionStartRequest autonomy(String value) { this.autonomy = value; return this; }
    public MissionStartRequest originRef(String value) { this.originRef = value; return this; }
    public MissionStartRequest expectedSettingsDigest(String value) {
        this.expectedSettingsDigest = value == null ? null : value.trim();
        return this;
    }
    public MissionStartRequest targets(List<MissionRevisionReference> value) { this.targets = value; return this; }
    public MissionStartRequest artifacts(List<MissionRevisionReference> value) { this.artifacts = value; return this; }
    public MissionStartRequest chain(List<String> value) { this.chain = value; return this; }
    public MissionStartRequest budget(long tokensTotal, long tokensPerDay, double usdCap) {
        if (tokensTotal < 0 || tokensPerDay < 0 || usdCap < 0 || !Double.isFinite(usdCap)) {
            throw new IllegalArgumentException("budget values must be finite and non-negative");
        }
        this.budgetTokensTotal = tokensTotal;
        this.budgetTokensPerDay = tokensPerDay;
        this.budgetUsdCap = usdCap;
        return this;
    }

    public Map<String, Object> getData() { return data; }
    public String getNamespace() { return namespace; }
    public String getProductId() { return productId; }
    public String getSubject() { return subject; }
    public String getKind() { return kind; }
    public String getGoal() { return goal; }
    public String getAutonomy() { return autonomy; }
    public String getOriginRef() { return originRef; }
    public String getExpectedSettingsDigest() { return expectedSettingsDigest; }
    public List<MissionRevisionReference> getTargets() { return targets; }
    public List<MissionRevisionReference> getArtifacts() { return artifacts; }
    public List<String> getChain() { return chain; }
    public long getBudgetTokensTotal() { return budgetTokensTotal; }
    public long getBudgetTokensPerDay() { return budgetTokensPerDay; }
    public double getBudgetUsdCap() { return budgetUsdCap; }
}
