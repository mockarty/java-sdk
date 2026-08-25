// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Request accepted by POST /api/v1/autotester/intents. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AutonomousMissionSubmitRequest {
    private String goal;
    @JsonProperty("productUrl") private String productUrl;
    @JsonProperty("traceId") private String traceId;
    @JsonProperty("dedupKey") private String dedupKey;
    @JsonProperty("missionId") private String missionId;
    private String autonomy;
    private List<String> options;
    @JsonProperty("contextRefs") private List<Map<String, String>> contextRefs;
    private Map<String, Object> budget;

    public AutonomousMissionSubmitRequest() {}

    public AutonomousMissionSubmitRequest goal(String value) {
        this.goal = value == null ? null : value.trim();
        return this;
    }
    public AutonomousMissionSubmitRequest productUrl(String value) { this.productUrl = value; return this; }
    public AutonomousMissionSubmitRequest traceId(String value) { this.traceId = value; return this; }
    public AutonomousMissionSubmitRequest dedupKey(String value) { this.dedupKey = value; return this; }
    public AutonomousMissionSubmitRequest missionId(String value) { this.missionId = value; return this; }
    public AutonomousMissionSubmitRequest autonomy(String value) { this.autonomy = value; return this; }
    public AutonomousMissionSubmitRequest options(List<String> value) { this.options = value; return this; }
    public AutonomousMissionSubmitRequest contextRefs(List<Map<String, String>> value) { this.contextRefs = value; return this; }
    public AutonomousMissionSubmitRequest budget(long tokensTotal, long tokensPerDay, double usdCap) {
        if (tokensTotal < 0 || tokensPerDay < 0 || usdCap < 0 || !Double.isFinite(usdCap)) {
            throw new IllegalArgumentException("budget values must be finite and non-negative");
        }
        Map<String, Object> value = new LinkedHashMap<>();
        if (tokensTotal != 0) value.put("tokens_total", tokensTotal);
        if (tokensPerDay != 0) value.put("tokens_per_day", tokensPerDay);
        if (usdCap != 0) value.put("usd_cap", usdCap);
        this.budget = value;
        return this;
    }

    public String getGoal() { return goal; }
    public String getProductUrl() { return productUrl; }
    public String getTraceId() { return traceId; }
    public String getDedupKey() { return dedupKey; }
    public String getMissionId() { return missionId; }
    public String getAutonomy() { return autonomy; }
    public List<String> getOptions() { return options; }
    public List<Map<String, String>> getContextRefs() { return contextRefs; }
    public Map<String, Object> getBudget() { return budget; }
}
