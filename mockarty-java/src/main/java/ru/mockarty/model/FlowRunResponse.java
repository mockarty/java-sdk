// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Response body for {@code POST /api/v1/api-tester/flow-runs}. Mirrors
 * the admin handler's {@code flowRunResponse} and the Go + Python SDK
 * field-for-field.
 *
 * <p>Wire-shape contract: {@code status} is one of "passed", "failed",
 * "broken". {@code durationMs} is reported in milliseconds (not the
 * Go-internal nanosecond {@code time.Duration}).</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowRunResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("durationMs")
    private long durationMs;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("variables")
    private Map<String, Object> variables;

    @JsonProperty("logs")
    private List<String> logs;

    @JsonProperty("errors")
    private List<String> errors;

    public String getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
    public String getStartedAt() { return startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public Map<String, Object> getVariables() { return variables; }
    public List<String> getLogs() { return logs; }
    public List<String> getErrors() { return errors; }

    public void setStatus(String s) { this.status = s; }
    public void setDurationMs(long d) { this.durationMs = d; }
    public void setStartedAt(String s) { this.startedAt = s; }
    public void setFinishedAt(String s) { this.finishedAt = s; }
    public void setVariables(Map<String, Object> v) { this.variables = v; }
    public void setLogs(List<String> l) { this.logs = l; }
    public void setErrors(List<String> e) { this.errors = e; }
}
