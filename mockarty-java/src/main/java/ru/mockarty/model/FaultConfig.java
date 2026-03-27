// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Configuration for a single fault injection in a chaos experiment.
 *
 * <p>Maps to the server-side FaultConfig struct in internal/chaos/models.go.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FaultConfig {

    @JsonProperty("type")
    private String type;

    @JsonProperty("parameters")
    private Map<String, Object> parameters;

    @JsonProperty("gracePeriodSec")
    private Integer gracePeriodSec;

    @JsonProperty("replicas")
    private Integer replicas;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("intervalSec")
    private Integer intervalSec;

    // Network-specific fields

    @JsonProperty("latencyMs")
    private Integer latencyMs;

    @JsonProperty("lossPercent")
    private Integer lossPercent;

    @JsonProperty("jitterMs")
    private Integer jitterMs;

    @JsonProperty("corruptPercent")
    private Integer corruptPercent;

    // Resource stress-specific fields

    @JsonProperty("cpuCores")
    private Integer cpuCores;

    @JsonProperty("memoryMB")
    private Integer memoryMB;

    @JsonProperty("stressType")
    private String stressType;

    // DNS-specific fields

    @JsonProperty("targetDomain")
    private String targetDomain;

    @JsonProperty("spoofIP")
    private String spoofIP;

    // IO chaos-specific fields

    @JsonProperty("ioLatencyMs")
    private Integer ioLatencyMs;

    @JsonProperty("ioErrPercent")
    private Integer ioErrPercent;

    @JsonProperty("ioPath")
    private String ioPath;

    // Time chaos-specific fields

    @JsonProperty("timeOffsetSec")
    private Integer timeOffsetSec;

    public FaultConfig() {
    }

    // Builder-style setters

    public FaultConfig type(String type) {
        this.type = type;
        return this;
    }

    public FaultConfig parameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public FaultConfig gracePeriodSec(Integer gracePeriodSec) {
        this.gracePeriodSec = gracePeriodSec;
        return this;
    }

    public FaultConfig replicas(Integer replicas) {
        this.replicas = replicas;
        return this;
    }

    public FaultConfig duration(String duration) {
        this.duration = duration;
        return this;
    }

    public FaultConfig intervalSec(Integer intervalSec) {
        this.intervalSec = intervalSec;
        return this;
    }

    public FaultConfig latencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
        return this;
    }

    public FaultConfig lossPercent(Integer lossPercent) {
        this.lossPercent = lossPercent;
        return this;
    }

    public FaultConfig jitterMs(Integer jitterMs) {
        this.jitterMs = jitterMs;
        return this;
    }

    public FaultConfig corruptPercent(Integer corruptPercent) {
        this.corruptPercent = corruptPercent;
        return this;
    }

    public FaultConfig cpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
        return this;
    }

    public FaultConfig memoryMB(Integer memoryMB) {
        this.memoryMB = memoryMB;
        return this;
    }

    public FaultConfig stressType(String stressType) {
        this.stressType = stressType;
        return this;
    }

    public FaultConfig targetDomain(String targetDomain) {
        this.targetDomain = targetDomain;
        return this;
    }

    public FaultConfig spoofIP(String spoofIP) {
        this.spoofIP = spoofIP;
        return this;
    }

    public FaultConfig ioLatencyMs(Integer ioLatencyMs) {
        this.ioLatencyMs = ioLatencyMs;
        return this;
    }

    public FaultConfig ioErrPercent(Integer ioErrPercent) {
        this.ioErrPercent = ioErrPercent;
        return this;
    }

    public FaultConfig ioPath(String ioPath) {
        this.ioPath = ioPath;
        return this;
    }

    public FaultConfig timeOffsetSec(Integer timeOffsetSec) {
        this.timeOffsetSec = timeOffsetSec;
        return this;
    }

    // Getters

    public String getType() {
        return type;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Integer getGracePeriodSec() {
        return gracePeriodSec;
    }

    public Integer getReplicas() {
        return replicas;
    }

    public String getDuration() {
        return duration;
    }

    public Integer getIntervalSec() {
        return intervalSec;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public Integer getLossPercent() {
        return lossPercent;
    }

    public Integer getJitterMs() {
        return jitterMs;
    }

    public Integer getCorruptPercent() {
        return corruptPercent;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public Integer getMemoryMB() {
        return memoryMB;
    }

    public String getStressType() {
        return stressType;
    }

    public String getTargetDomain() {
        return targetDomain;
    }

    public String getSpoofIP() {
        return spoofIP;
    }

    public Integer getIoLatencyMs() {
        return ioLatencyMs;
    }

    public Integer getIoErrPercent() {
        return ioErrPercent;
    }

    public String getIoPath() {
        return ioPath;
    }

    public Integer getTimeOffsetSec() {
        return timeOffsetSec;
    }

    @Override
    public String toString() {
        return "FaultConfig{" +
                "type='" + type + '\'' +
                ", latencyMs=" + latencyMs +
                ", lossPercent=" + lossPercent +
                '}';
    }
}
