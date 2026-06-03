// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Wire shape for {@code POST /api/v1/namespaces/:namespace/tcm/external-runs}.
 *
 * <p>Defined server-side in {@code internal/testcase/external_run.go}.
 * Field names map to the Go struct's JSON tags. Defaults match the
 * 80%-case rule — most callers only set {@code status} and one of
 * {@code caseId} / {@code caseName}.</p>
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ExternalRunRequest {

    /** Schema version this build of the SDK speaks. */
    public static final int SCHEMA_VERSION = 1;

    public static final String STATUS_PASSED = "passed";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_BROKEN = "broken";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_CANCELLED = "cancelled";

    @JsonProperty("schemaVersion")
    private int schemaVersion = SCHEMA_VERSION;

    @JsonProperty("caseId")
    private String caseId;

    /**
     * Author-pinned identity (Allure {@code testCaseId} / {@code @allure.id}).
     * Distinct from {@code caseId} (Mockarty's internal UUID): the server
     * resolves on this BEFORE {@code fullName}/{@code caseName} (migration 402),
     * so a method/parameter rename still lands on the same case.
     */
    @JsonProperty("testCaseId")
    private String testCaseId;

    /**
     * Deterministic test identifier ("package.Class::test[param=value]") used
     * for duplicate-prevention across parallel CI workers. Backed by the
     * {@code test_cases.external_full_name} column + partial unique index
     * (migration 321).
     */
    @JsonProperty("fullName")
    private String fullName;

    @JsonProperty("caseName")
    private String caseName;

    /** Markdown description stamped on case auto-create (or overwrite when
     * {@code claimCaseOwnership}). */
    @JsonProperty("caseDescription")
    private String caseDescription;

    /** Review-workflow expected-result clause (Mockarty's differentiator vs
     * plain Allure). */
    @JsonProperty("caseExpectedResult")
    private String caseExpectedResult;

    @JsonProperty("planId")
    private String planId;

    @JsonProperty("autoCreate")
    private boolean autoCreate;

    @JsonProperty("status")
    private String status;

    @JsonProperty("framework")
    private String framework;

    @JsonProperty("frameworkVersion")
    private String frameworkVersion;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("testDisplayName")
    private String testDisplayName;

    @JsonProperty("durationMs")
    private long durationMs;

    @JsonProperty("error")
    private String error;

    @JsonProperty("stdout")
    private String stdout;

    @JsonProperty("stderr")
    private String stderr;

    @JsonProperty("startedAt")
    private String startedAt;

    @JsonProperty("finishedAt")
    private String finishedAt;

    @JsonProperty("labels")
    private Map<String, String> labels;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("steps")
    private List<ExternalStep> steps;

    @JsonProperty("attachments")
    private List<ExternalAttachment> attachments;

    /** Key/value tags persisted to {@code test_cases.custom_fields_json}. */
    @JsonProperty("customFields")
    private List<CustomField> customFields;

    /** When true, the receiver overwrites existing Description /
     * ExpectedResult / CustomFields on every upload so the code annotation is
     * source-of-truth. Default false preserves manual UI edits. */
    @JsonProperty("claimCaseOwnership")
    private boolean claimCaseOwnership;

    public ExternalRunRequest() {}

    // ── Fluent setters ──────────────────────────────────────────────

    public ExternalRunRequest status(String s) { this.status = s; return this; }
    public ExternalRunRequest caseId(String id) { this.caseId = id; return this; }
    public ExternalRunRequest testCaseId(String id) { this.testCaseId = id; return this; }
    public ExternalRunRequest fullName(String n) { this.fullName = n; return this; }
    public ExternalRunRequest caseName(String n) { this.caseName = n; return this; }
    public ExternalRunRequest caseDescription(String d) { this.caseDescription = d; return this; }
    public ExternalRunRequest caseExpectedResult(String r) { this.caseExpectedResult = r; return this; }
    public ExternalRunRequest planId(String p) { this.planId = p; return this; }
    public ExternalRunRequest autoCreate(boolean v) { this.autoCreate = v; return this; }
    public ExternalRunRequest framework(String f) { this.framework = f; return this; }
    public ExternalRunRequest frameworkVersion(String v) { this.frameworkVersion = v; return this; }
    public ExternalRunRequest externalId(String id) { this.externalId = id; return this; }
    public ExternalRunRequest testDisplayName(String n) { this.testDisplayName = n; return this; }
    public ExternalRunRequest durationMs(long ms) { this.durationMs = ms; return this; }
    public ExternalRunRequest error(String e) { this.error = e; return this; }
    public ExternalRunRequest stdout(String s) { this.stdout = s; return this; }
    public ExternalRunRequest stderr(String s) { this.stderr = s; return this; }
    public ExternalRunRequest startedAt(String s) { this.startedAt = s; return this; }
    public ExternalRunRequest finishedAt(String s) { this.finishedAt = s; return this; }
    public ExternalRunRequest labels(Map<String, String> v) { this.labels = v; return this; }
    public ExternalRunRequest metadata(Map<String, Object> v) { this.metadata = v; return this; }
    public ExternalRunRequest steps(List<ExternalStep> v) { this.steps = v; return this; }
    public ExternalRunRequest attachments(List<ExternalAttachment> v) { this.attachments = v; return this; }
    public ExternalRunRequest customFields(List<CustomField> v) { this.customFields = v; return this; }
    public ExternalRunRequest claimCaseOwnership(boolean v) { this.claimCaseOwnership = v; return this; }

    // ── Getters ─────────────────────────────────────────────────────

    public int getSchemaVersion() { return schemaVersion; }
    public String getCaseId() { return caseId; }
    public String getTestCaseId() { return testCaseId; }
    public String getFullName() { return fullName; }
    public String getCaseName() { return caseName; }
    public String getCaseDescription() { return caseDescription; }
    public String getCaseExpectedResult() { return caseExpectedResult; }
    public String getPlanId() { return planId; }
    public boolean isAutoCreate() { return autoCreate; }
    public String getStatus() { return status; }
    public String getFramework() { return framework; }
    public String getFrameworkVersion() { return frameworkVersion; }
    public String getExternalId() { return externalId; }
    public String getTestDisplayName() { return testDisplayName; }
    public long getDurationMs() { return durationMs; }
    public String getError() { return error; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public String getStartedAt() { return startedAt; }
    public String getFinishedAt() { return finishedAt; }
    public Map<String, String> getLabels() { return labels; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<ExternalStep> getSteps() { return steps; }
    public List<ExternalAttachment> getAttachments() { return attachments; }
    public List<CustomField> getCustomFields() { return customFields; }
    public boolean isClaimCaseOwnership() { return claimCaseOwnership; }
}
