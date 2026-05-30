// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body for {@code POST /tcm/external-runs}. Mirrors the Go
 * server's {@code ExternalRunResult} struct — see
 * {@code internal/testcase/external_run.go}.
 */
public class ExternalRunResponse {

    @JsonProperty("runId")
    private String runId;

    @JsonProperty("caseId")
    private String caseId;

    @JsonProperty("caseName")
    private String caseName;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("status")
    private String status;

    @JsonProperty("url")
    private String url;

    @JsonProperty("resolved")
    private String resolved;

    @JsonProperty("startedAt")
    private String startedAt;

    public String getRunId() { return runId; }
    public String getCaseId() { return caseId; }
    public String getCaseName() { return caseName; }
    public String getNamespace() { return namespace; }
    public String getStatus() { return status; }
    public String getUrl() { return url; }
    public String getResolved() { return resolved; }
    public String getStartedAt() { return startedAt; }

    public void setRunId(String s) { this.runId = s; }
    public void setCaseId(String s) { this.caseId = s; }
    public void setCaseName(String s) { this.caseName = s; }
    public void setNamespace(String s) { this.namespace = s; }
    public void setStatus(String s) { this.status = s; }
    public void setUrl(String s) { this.url = s; }
    public void setResolved(String s) { this.resolved = s; }
    public void setStartedAt(String s) { this.startedAt = s; }
}
