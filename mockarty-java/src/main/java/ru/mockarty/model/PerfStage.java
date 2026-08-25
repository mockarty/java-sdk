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

/** One virtual-user or arrival-rate ramp stage in a saved perf profile. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfStage {
    private static final Set<String> TYPED_JSON_NAMES = Set.of(
            "duration", "target", "targetRPS", "targetRps");

    @JsonProperty("duration") private String duration;
    @JsonProperty("target") private Integer target;
    @JsonProperty("targetRPS") private Integer targetRps;
    @JsonIgnore private final Map<String, Object> extra = new LinkedHashMap<>();

    public PerfStage duration(String value) { this.duration = value; return this; }
    public PerfStage target(Integer value) { this.target = value; return this; }
    public PerfStage targetRps(Integer value) { this.targetRps = value; return this; }

    public String getDuration() { return duration; }
    /** Returns the server default (zero) when a partial response omits target. */
    public int getTarget() { return target == null ? 0 : target; }
    public Integer getTargetRps() { return targetRps; }

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
