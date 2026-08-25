// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** User-manageable defaults and evidence retention for autonomous missions. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AutonomyNamespaceSettings {
    @JsonProperty("defaultBudget")
    private Map<String, Object> defaultBudget;
    @JsonProperty("defaultContextRefs")
    private List<Map<String, String>> defaultContextRefs;
    @JsonProperty("defaultAutonomy")
    private String defaultAutonomy;
    @JsonProperty("updatedAt")
    private String updatedAt;
    @JsonProperty("journalEventRetentionDays")
    private Integer journalEventRetentionDays;
    @JsonProperty("journalPayloadRetentionDays")
    private Integer journalPayloadRetentionDays;
    @JsonProperty("runWindowMinutes")
    private Integer runWindowMinutes;
    @JsonProperty("etag")
    private String etag;

    public AutonomyNamespaceSettings defaultAutonomy(String value) {
        this.defaultAutonomy = value;
        return this;
    }

    public AutonomyNamespaceSettings defaultBudget(Map<String, Object> value) {
        this.defaultBudget = value;
        return this;
    }

    public AutonomyNamespaceSettings defaultContextRefs(List<Map<String, String>> value) {
        this.defaultContextRefs = value;
        return this;
    }

    public AutonomyNamespaceSettings journalEventRetentionDays(Integer value) {
        this.journalEventRetentionDays = value;
        return this;
    }

    public AutonomyNamespaceSettings journalPayloadRetentionDays(Integer value) {
        this.journalPayloadRetentionDays = value;
        return this;
    }

    public AutonomyNamespaceSettings runWindowMinutes(Integer value) {
        this.runWindowMinutes = value;
        return this;
    }

    public Map<String, Object> getDefaultBudget() { return defaultBudget; }
    public List<Map<String, String>> getDefaultContextRefs() { return defaultContextRefs; }
    public String getDefaultAutonomy() { return defaultAutonomy; }
    public String getUpdatedAt() { return updatedAt; }
    public Integer getJournalEventRetentionDays() { return journalEventRetentionDays; }
    public Integer getJournalPayloadRetentionDays() { return journalPayloadRetentionDays; }
    public Integer getRunWindowMinutes() { return runWindowMinutes; }
    public String getEtag() { return etag; }
}
