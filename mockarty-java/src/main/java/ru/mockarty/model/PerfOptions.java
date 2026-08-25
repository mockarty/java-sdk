// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Typed {@code options} envelope for a saved performance configuration. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerfOptions {
    private static final Set<String> TYPED_JSON_NAMES = Set.of(
            "thresholds", "duration", "stages", "abortCriteria", "gracefulStop", "gracefulRampDown",
            "metricsPush", "metricsPushInterval", "startAtUnixMs", "vus", "iterations", "rps",
            "maxVUs", "maxVus", "arrivalRate", "emitHistograms");

    @JsonProperty("thresholds") private Map<String, List<String>> thresholds;
    @JsonProperty("duration") private String duration;
    @JsonProperty("stages") private List<PerfStage> stages;
    @JsonProperty("abortCriteria") private List<AbortCriterion> abortCriteria;
    @JsonProperty("gracefulStop") private String gracefulStop;
    @JsonProperty("gracefulRampDown") private String gracefulRampDown;
    @JsonProperty("metricsPush") private List<String> metricsPush;
    @JsonProperty("metricsPushInterval") private String metricsPushInterval;
    @JsonProperty("startAtUnixMs") private Long startAtUnixMs;
    @JsonProperty("vus") private Integer vus;
    @JsonProperty("iterations") private Integer iterations;
    @JsonProperty("rps") private Integer rps;
    @JsonIgnore private Integer maxVus;
    @JsonIgnore private boolean canonicalMaxVUsSeen;
    @JsonProperty("arrivalRate") private Boolean arrivalRate;
    @JsonProperty("emitHistograms") private Boolean emitHistograms;
    @JsonIgnore private final Map<String, Object> extra = new LinkedHashMap<>();

    public PerfOptions thresholds(Map<String, List<String>> value) { this.thresholds = value; return this; }
    public PerfOptions duration(String value) { this.duration = value; return this; }
    public PerfOptions stages(List<PerfStage> value) { this.stages = value; return this; }
    public PerfOptions abortCriteria(List<AbortCriterion> value) { this.abortCriteria = value; return this; }
    public PerfOptions gracefulStop(String value) { this.gracefulStop = value; return this; }
    public PerfOptions gracefulRampDown(String value) { this.gracefulRampDown = value; return this; }
    public PerfOptions metricsPush(List<String> value) { this.metricsPush = value; return this; }
    public PerfOptions metricsPushInterval(String value) { this.metricsPushInterval = value; return this; }
    public PerfOptions startAtUnixMs(Long value) { this.startAtUnixMs = value; return this; }
    public PerfOptions vus(Integer value) { this.vus = value; return this; }
    public PerfOptions iterations(Integer value) { this.iterations = value; return this; }
    public PerfOptions rps(Integer value) { this.rps = value; return this; }
    public PerfOptions maxVus(Integer value) { this.maxVus = value; return this; }
    public PerfOptions arrivalRate(Boolean value) { this.arrivalRate = value; return this; }
    public PerfOptions emitHistograms(Boolean value) { this.emitHistograms = value; return this; }

    public Map<String, List<String>> getThresholds() { return thresholds; }
    public String getDuration() { return duration; }
    public List<PerfStage> getStages() { return stages; }
    public List<AbortCriterion> getAbortCriteria() { return abortCriteria; }
    public String getGracefulStop() { return gracefulStop; }
    public String getGracefulRampDown() { return gracefulRampDown; }
    public List<String> getMetricsPush() { return metricsPush; }
    public String getMetricsPushInterval() { return metricsPushInterval; }
    public Long getStartAtUnixMs() { return startAtUnixMs; }
    public Integer getVus() { return vus; }
    public Integer getIterations() { return iterations; }
    public Integer getRps() { return rps; }
    @JsonProperty("maxVUs")
    public Integer getMaxVus() { return maxVus; }
    public Boolean getArrivalRate() { return arrivalRate; }
    public Boolean getEmitHistograms() { return emitHistograms; }

    /** Canonical spelling always wins, including an explicit JSON null. */
    @JsonProperty("maxVUs")
    public void setCanonicalMaxVUs(Integer value) {
        maxVus = value;
        canonicalMaxVUsSeen = true;
    }

    /** Historical read-only alias; ignored after the canonical key was seen. */
    @JsonProperty("maxVus")
    public void setLegacyMaxVus(Integer value) {
        if (!canonicalMaxVUsSeen) {
            maxVus = value;
        }
    }

    /** Retains newer server options during a GET -> model -> PUT cycle. */
    @JsonAnySetter
    public void putExtra(String name, Object value) {
        if (!PerfJsonExtras.isReserved(TYPED_JSON_NAMES, name)) {
            extra.put(name, value);
        }
    }

    /** Emits retained newer-server options without replacing typed fields. */
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return PerfJsonExtras.immutableCopy(extra);
    }
}
