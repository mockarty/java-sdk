// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalStep;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers that bridge a {@link Tester} chain into the
 * {@link ru.mockarty.api.ExternalRunsApi} payload. Mirrors
 * {@code sdk/go-sdk/tester/external_run.go} and
 * {@code sdk/py-sdk/src/mockarty/tester/external_run.py} so test
 * suites translate 1:1 across all three SDKs.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * Tester t = new Tester.Builder().baseUrl("http://...").build();
 * t.http().get("/me").expectStatus(200);
 * t.finish();
 *
 * client.externalRuns().report("qa",
 *     ExternalRunBridge.toExternalRunRequest(t,
 *         new ExternalRunBridge.Options()
 *             .caseName("me-endpoint")
 *             .autoCreate(true)));
 * }</pre>
 */
public final class ExternalRunBridge {

    private ExternalRunBridge() {}

    /** Options collected at the report call-site — caller binds the run to
     *  a case / plan, supplies labels + metadata, marks auto-create. All
     *  fields optional; see Go SDK {@code ExternalRunOptions}. */
    public static final class Options {
        String caseId;
        String caseName;
        String fullName;
        String testDisplayName;
        String planId;
        String planRunId;
        String framework;
        String frameworkVersion;
        String externalId;
        boolean autoCreate;
        boolean claimCaseOwnership;
        Map<String, String> labels;
        Map<String, Object> metadata;

        public Options caseId(String v) { this.caseId = v; return this; }
        public Options caseName(String v) { this.caseName = v; return this; }
        public Options fullName(String v) { this.fullName = v; return this; }
        public Options testDisplayName(String v) { this.testDisplayName = v; return this; }
        public Options planId(String v) { this.planId = v; return this; }
        public Options planRunId(String v) { this.planRunId = v; return this; }
        public Options framework(String v) { this.framework = v; return this; }
        public Options frameworkVersion(String v) { this.frameworkVersion = v; return this; }
        public Options externalId(String v) { this.externalId = v; return this; }
        public Options autoCreate(boolean v) { this.autoCreate = v; return this; }
        public Options claimCaseOwnership(boolean v) { this.claimCaseOwnership = v; return this; }
        public Options labels(Map<String, String> v) { this.labels = v; return this; }
        public Options metadata(Map<String, Object> v) { this.metadata = v; return this; }
    }

    /**
     * Materialise the Tester report into an ExternalRunRequest ready
     * for {@code client.externalRuns().report(namespace, req)}.
     */
    public static ExternalRunRequest toExternalRunRequest(Tester t, Options opts) {
        if (opts == null) { opts = new Options(); }
        List<StepRecord> report = t.report();

        ExternalRunRequest req = new ExternalRunRequest()
                .status(t.ok() ? ExternalRunRequest.STATUS_PASSED : ExternalRunRequest.STATUS_FAILED)
                .caseId(opts.caseId)
                .caseName(opts.caseName)
                .testDisplayName(opts.testDisplayName)
                .planId(opts.planId)
                .framework(opts.framework != null ? opts.framework : "mockarty-tester-java")
                .frameworkVersion(opts.frameworkVersion)
                .externalId(opts.externalId)
                .autoCreate(opts.autoCreate)
                .labels(opts.labels)
                .metadata(opts.metadata);

        if (!t.ok()) {
            List<String> errs = t.errors();
            if (!errs.isEmpty()) {
                req.error(errs.get(0));
            }
        }

        if (!report.isEmpty()) {
            StepRecord first = report.get(0);
            StepRecord last = report.get(report.size() - 1);
            if (!Instant.EPOCH.equals(first.startedAt)) {
                req.startedAt(iso(first.startedAt));
            }
            if (!Instant.EPOCH.equals(last.endedAt)) {
                req.finishedAt(iso(last.endedAt));
                if (!Instant.EPOCH.equals(first.startedAt)) {
                    req.durationMs(java.time.Duration.between(first.startedAt, last.endedAt).toMillis());
                }
            }
            List<ExternalStep> steps = new ArrayList<>(report.size());
            for (StepRecord r : report) {
                steps.add(toExternalStep(r));
            }
            req.steps(steps);
        }
        return req;
    }

    private static ExternalStep toExternalStep(StepRecord r) {
        ExternalStep step = new ExternalStep()
                .name(r.name);
        if (r.failures.isEmpty()) {
            step.status(ExternalRunRequest.STATUS_PASSED);
        } else {
            step.status(ExternalRunRequest.STATUS_FAILED);
            step.error(joinFailures(r.failures));
        }
        if (!Instant.EPOCH.equals(r.startedAt)) {
            step.startedAt(iso(r.startedAt));
        }
        if (!Instant.EPOCH.equals(r.endedAt)) {
            step.finishedAt(iso(r.endedAt));
            if (!Instant.EPOCH.equals(r.startedAt)) {
                step.durationMs(java.time.Duration.between(r.startedAt, r.endedAt).toMillis());
            }
        }
        step.metadata(stepMetadata(r));
        return step;
    }

    private static Map<String, Object> stepMetadata(StepRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("protocol", r.protocol);
        m.put("method", r.method);
        if (r.url != null && !r.url.isEmpty()) {
            m.put("url", r.url);
        }
        if (r.statusOrCode != 0) {
            m.put("statusOrCode", r.statusOrCode);
        }
        return m;
    }

    private static String joinFailures(List<String> fs) {
        if (fs.isEmpty()) { return ""; }
        if (fs.size() == 1) { return fs.get(0); }
        StringBuilder b = new StringBuilder(fs.get(0));
        for (int i = 1; i < fs.size(); i++) {
            b.append("; ").append(fs.get(i));
        }
        return b.toString();
    }

    private static String iso(Instant t) {
        return DateTimeFormatter.ISO_INSTANT.format(t);
    }
}
