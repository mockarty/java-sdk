// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** One step inside an {@link ExternalRunRequest}. */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ExternalStep {

    @JsonProperty("name")
    private String name;

    @JsonProperty("status")
    private String status;

    @JsonProperty("error")
    private String error;

    @JsonProperty("durationMs")
    private long durationMs;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public ExternalStep() {}

    public ExternalStep(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public ExternalStep name(String n) { this.name = n; return this; }
    public ExternalStep status(String s) { this.status = s; return this; }
    public ExternalStep error(String e) { this.error = e; return this; }
    public ExternalStep durationMs(long ms) { this.durationMs = ms; return this; }
    public ExternalStep startedAt(String s) { this.startedAt = s; return this; }
    public ExternalStep finishedAt(String s) { this.finishedAt = s; return this; }
    public ExternalStep metadata(Map<String, Object> m) { this.metadata = m; return this; }

    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getError() { return error; }
    public long getDurationMs() { return durationMs; }
    public String getStartedAt() { return startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public Map<String, Object> getMetadata() { return metadata; }
}
