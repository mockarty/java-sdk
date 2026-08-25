// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** An automatic stop condition for a performance test. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbortCriterion {
    private static final Set<String> TYPED_JSON_NAMES = Set.of(
            "metric", "stat", "condition", "duration", "name", "value", "enabled");

    @JsonProperty("metric") private String metric;
    @JsonProperty("stat") private String stat;
    @JsonProperty("condition") private String condition;
    @JsonProperty("duration") private String duration;
    @JsonProperty("name") private String name;
    @JsonProperty("value") private Double value;
    @JsonProperty("enabled") private Boolean enabled;
    @JsonIgnore private final Map<String, Object> extra = new LinkedHashMap<>();

    public AbortCriterion metric(String value) { this.metric = value; return this; }
    public AbortCriterion stat(String value) { this.stat = value; return this; }
    public AbortCriterion condition(String value) { this.condition = value; return this; }
    public AbortCriterion duration(String value) { this.duration = value; return this; }
    public AbortCriterion name(String value) { this.name = value; return this; }
    public AbortCriterion value(Double value) { this.value = value; return this; }
    public AbortCriterion enabled(Boolean value) { this.enabled = value; return this; }

    public String getMetric() { return metric; }
    public String getStat() { return stat; }
    public String getCondition() { return condition; }
    public String getDuration() { return duration; }
    public String getName() { return name; }
    public Double getValue() { return value; }
    /** Returns the server default (false) when a partial response omits enabled. */
    public boolean getEnabled() { return Boolean.TRUE.equals(enabled); }

    @JsonAnySetter
    public void putExtra(String name, Object value) {
        if (!PerfJsonExtras.isReserved(TYPED_JSON_NAMES, name)) {
            extra.put(name, value);
        }
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return PerfJsonExtras.immutableCopy(extra);
    }
}
