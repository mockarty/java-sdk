// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.
package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/** The durable receipt of a finished command: what CI cites as evidence. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MeshExecutionReport {
    private String id;
    private String ref;
    private String namespace;
    private String deviceId;
    private String submitterUserId;
    private String state;
    private String summary;
    private String failureCode;
    private String reportKind;
    private String subjectId;
    private Instant recordedAt;
    private Instant startedAt;
    private Instant finishedAt;

    public String getId() { return id; }
    public String getRef() { return ref; }
    public String getNamespace() { return namespace; }
    public String getDeviceId() { return deviceId; }
    public String getSubmitterUserId() { return submitterUserId; }
    public String getState() { return state; }
    public String getSummary() { return summary; }
    public String getFailureCode() { return failureCode; }
    public String getReportKind() { return reportKind; }
    public String getSubjectId() { return subjectId; }
    public Instant getRecordedAt() { return recordedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
