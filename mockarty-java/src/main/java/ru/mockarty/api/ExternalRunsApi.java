// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.AllureUploadEmptyException;
import ru.mockarty.exception.AllureUploadPartialException;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ExternalAttachment;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalRunResponse;
import ru.mockarty.model.ExternalStep;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Client for {@code POST /api/v1/namespaces/:namespace/tcm/external-runs}.
 *
 * <p>Used by the JUnit 5 framework adapter (and direct callers) to ship
 * a per-test outcome — status, steps, captured stdout/stderr, small
 * attachments — to TCM as a synthetic case run, without invoking the
 * orchestrator.</p>
 *
 * <p>Defaults are tuned for the 80% case: caller sets {@code status} and
 * one of {@code caseId} / {@code caseName} on the request and that's it.
 * Empty optional fields are dropped from the wire payload by the
 * {@code @JsonInclude(NON_DEFAULT)} on {@link ExternalRunRequest}.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * ExternalRunResponse resp = client.externalRuns().report("qa",
 *     new ExternalRunRequest()
 *         .status(ExternalRunRequest.STATUS_PASSED)
 *         .caseId("11111111-1111-1111-1111-111111111111")
 *         .framework("junit5")
 *         .externalId("ru.example.LoginTest#login"));
 * System.out.println("Run url: " + resp.getUrl());
 * }</pre>
 */
public class ExternalRunsApi {

    private final MockartyClient client;

    public ExternalRunsApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Persist a synthetic case run.
     *
     * @param namespace target namespace; required.
     * @param request   the run envelope; must carry {@code status} plus
     *                  one of {@code caseId} / {@code caseName}.
     * @return the server's response (run id, resolved case, deep-link URL).
     */
    public ExternalRunResponse report(String namespace, ExternalRunRequest request)
            throws MockartyException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            throw new IllegalArgumentException("request.status is required");
        }
        if ((request.getCaseId() == null || request.getCaseId().isEmpty())
                && (request.getCaseName() == null || request.getCaseName().isEmpty())) {
            throw new IllegalArgumentException(
                    "one of request.caseId / request.caseName is required");
        }
        String path = "/api/v1/namespaces/" + namespace + "/tcm/external-runs";
        return client.post(path, request, ExternalRunResponse.class);
    }

    /**
     * POST a batch of external-run results to
     * {@code /tcm/external-runs/batch} in one round-trip.
     *
     * <p>Fan-in endpoint for CI scripts that produce many results per
     * pipeline. The server caps the batch at 100 items per call —
     * larger sets must be chunked by the caller. Even when N items
     * fail the server returns 200 with per-row errors; inspect the
     * returned {@link JsonNode}'s {@code results[i].error} to
     * correlate.</p>
     *
     * <p>Returns the raw response envelope:</p>
     * <pre>{@code
     * {
     *   "results": [
     *     {"index": 0, "result": {"runId": "...", "caseId": "..."}},
     *     {"index": 1, "error": "..."}
     *   ],
     *   "counts": {"total": 2, "passed": 1, "failed": 1}
     * }
     * }</pre>
     *
     * @param namespace target namespace; required.
     * @param requests  non-empty list of run envelopes; same shape as
     *                  {@link #report(String, ExternalRunRequest)}.
     * @return the raw JSON envelope described above.
     */
    public JsonNode reportBatch(String namespace, List<ExternalRunRequest> requests)
            throws MockartyException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("requests must be a non-empty list");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("runs", requests);
        String path = "/api/v1/namespaces/" + namespace + "/tcm/external-runs/batch";
        return client.post(path, body, JsonNode.class);
    }

    /**
     * Bulk-upload every {@code <uuid>-result.json} file in an Allure
     * results directory to {@link #report(String, ExternalRunRequest)}.
     *
     * <p>Use this from CI after the JVM-side run finishes:</p>
     * <pre>{@code
     * client.externalRuns().uploadAllureDir("qa", Paths.get("allure-results"));
     * }</pre>
     *
     * <p>Mapping rules ({@code allure → TCM external-runs}):</p>
     * <ul>
     *   <li>{@code uuid} → externalId.</li>
     *   <li>{@code name} → testDisplayName.</li>
     *   <li>{@code name} → caseName, {@code fullName} → fullName (the
     *       server's deterministic dedup key), with autoCreate=true so missing
     *       cases get materialised.</li>
     *   <li>{@code status} → status (mapped lowercase wire form).</li>
     *   <li>{@code statusDetails.message + trace} → error.</li>
     *   <li>{@code stop - start} → durationMs.</li>
     *   <li>{@code testCaseId}, or the {@code AS_ID} / {@code ALLURE_ID}
     *       label an Allure TestOps adapter writes for {@code @AllureId},
     *       → testCaseId (the author-pinned identity — NOT caseId, which is
     *       Mockarty's internal case UUID).</li>
     *   <li>{@code steps[]} → flat ExternalStep list (nested steps are
     *       hoisted with name prefixed by the parent's name).</li>
     *   <li>Attachment metadata is shipped as {@link ExternalAttachment}
     *       — bytes loaded from the {@code source} file in the same
     *       directory; missing files are skipped fail-soft.</li>
     * </ul>
     *
     * <p>Upload is best-effort per file so a single malformed
     * {@code *-result.json} does not kill a CI upload — but a partial upload
     * is NOT reported as a success: when any file is skipped the method throws
     * {@link AllureUploadPartialException}, which carries both the skipped
     * entries and the responses that did land. A directory holding no results
     * at all throws {@link AllureUploadEmptyException}, and a missing directory
     * throws {@link FileNotFoundException} — a CI step that silently uploads
     * nothing is a green pipeline hiding a test run that produced nothing.</p>
     *
     * @throws FileNotFoundException        {@code resultsDir} is null or not a directory
     * @throws AllureUploadEmptyException   the directory holds no {@code *-result.json}
     * @throws AllureUploadPartialException at least one result was not reported
     */
    public List<ExternalRunResponse> uploadAllureDir(String namespace, Path resultsDir)
            throws IOException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (resultsDir == null || !Files.isDirectory(resultsDir)) {
            throw new FileNotFoundException(
                    "mockarty: allure-results directory not found: " + resultsDir);
        }
        ObjectMapper mapper = new ObjectMapper();
        List<ExternalRunResponse> out = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                resultsDir, "*-result.json")) {
            for (Path file : stream) {
                files.add(file);
            }
        }
        // DirectoryStream order is filesystem-dependent; sort so the upload
        // order matches the Go/Python implementations.
        Collections.sort(files);
        for (Path file : files) {
            try {
                JsonNode root = mapper.readTree(file.toFile());
                ExternalRunRequest req = allureToExternalRun(root, resultsDir, mapper);
                out.add(report(namespace, req));
            } catch (Throwable t) {
                // Fail-soft per-file: the loop continues so one bad
                // result.json doesn't kill the CI upload. Every skip is
                // recorded and surfaced below — never silently dropped.
                skipped.add(file.getFileName() + ": " + t.getMessage());
                System.err.println("[mockarty] skipped Allure file "
                        + file + ": " + t.getMessage());
            }
        }
        if (!skipped.isEmpty()) {
            throw new AllureUploadPartialException(out, skipped);
        }
        if (files.isEmpty()) {
            throw new AllureUploadEmptyException("mockarty: no *-result.json in "
                    + resultsDir + " — nothing was reported to Mockarty");
        }
        return out;
    }

    /**
     * Convert an Allure {@code TestResult} JSON tree into an
     * {@link ExternalRunRequest}. Package-visible for unit testing.
     */
    static ExternalRunRequest allureToExternalRun(JsonNode root, Path resultsDir, ObjectMapper mapper)
            throws IOException {
        ExternalRunRequest req = new ExternalRunRequest();
        String uuid = root.path("uuid").asText("");
        String name = root.path("name").asText("");
        String fullName = root.path("fullName").asText(name);
        String status = root.path("status").asText("unknown");
        // Map Allure status → ExternalRunRequest status. Allure has
        // {passed, failed, broken, skipped, unknown}; ER has same set.
        req.status(mapStatus(status));
        req.framework("allure");
        req.frameworkVersion("2");
        req.externalId(uuid);
        req.testDisplayName(name);
        req.caseName(name.isEmpty() ? fullName : name);
        // fullName is the server's deterministic dedup key (external_full_name).
        // Without it every re-run of the same suite resolved by display name
        // only, so a renamed or duplicate-named test forked a second case.
        if (!fullName.isEmpty()) {
            req.fullName(fullName);
        }
        req.autoCreate(true);

        // Author-pinned identity. Allure's own field is `testCaseId`; Allure
        // TestOps adapters express `@AllureId(123)` as the `AS_ID` label
        // instead. Both must land on testCaseId — NOT on caseId, which is
        // Mockarty's internal case UUID: sending "123" there made the server
        // look up a non-existent UUID and fall through, and pinning
        // autoCreate(false) alongside it meant the result was dropped outright.
        String pinnedId = root.path("testCaseId").asText("");
        JsonNode labels = root.path("labels");
        if (labels.isArray()) {
            Map<String, String> firstByName = new HashMap<>();
            Iterator<JsonNode> it = labels.elements();
            while (it.hasNext()) {
                JsonNode l = it.next();
                String lname = l.path("name").asText("");
                String lvalue = l.path("value").asText("");
                String lkey = lname.toLowerCase(java.util.Locale.ROOT);
                if (("as_id".equals(lkey) || "allure_id".equals(lkey) || "allureid".equals(lkey))
                        && !lvalue.isEmpty()) {
                    pinnedId = lvalue;
                }
                if (!firstByName.containsKey(lname)) {
                    firstByName.put(lname, lvalue);
                }
            }
        }
        if (!pinnedId.isEmpty()) {
            req.testCaseId(pinnedId);
        }

        long start = root.path("start").asLong(0);
        long stop = root.path("stop").asLong(0);
        if (stop > start && start > 0) {
            req.durationMs(stop - start);
        }
        if (start > 0) {
            req.startedAt(java.time.Instant.ofEpochMilli(start).toString());
        }
        if (stop > 0) {
            req.finishedAt(java.time.Instant.ofEpochMilli(stop).toString());
        }
        JsonNode sd = root.path("statusDetails");
        if (sd.isObject()) {
            String msg = sd.path("message").asText("");
            String trace = sd.path("trace").asText("");
            String err = (msg + (trace.isEmpty() ? "" : "\n" + trace)).trim();
            if (!err.isEmpty()) {
                req.error(err);
            }
        }
        JsonNode steps = root.path("steps");
        if (steps.isArray() && steps.size() > 0) {
            List<ExternalStep> out = new ArrayList<>();
            flattenSteps(steps, "", out);
            req.steps(out);
        }
        JsonNode atts = root.path("attachments");
        if (atts.isArray() && atts.size() > 0) {
            List<ExternalAttachment> wire = new ArrayList<>();
            Iterator<JsonNode> it = atts.elements();
            while (it.hasNext()) {
                JsonNode a = it.next();
                String source = a.path("source").asText("");
                String aname = a.path("name").asText("");
                String type = a.path("type").asText("application/octet-stream");
                ExternalAttachment ea = new ExternalAttachment()
                        .name(aname.isEmpty() ? source : aname)
                        .contentType(type);
                if (!source.isEmpty()) {
                    Path body = resultsDir.resolve(source);
                    if (Files.isRegularFile(body)) {
                        ea.body(Files.readAllBytes(body));
                    }
                }
                wire.add(ea);
            }
            req.attachments(wire);
        }
        // Carry labels + parameters into metadata for richer reporting.
        Map<String, Object> metadata = new HashMap<>();
        if (labels.isArray()) {
            List<Map<String, String>> ml = mapper.convertValue(labels,
                    new TypeReference<List<Map<String, String>>>() {});
            metadata.put("labels", ml);
        }
        JsonNode links = root.path("links");
        if (links.isArray() && links.size() > 0) {
            metadata.put("links", mapper.convertValue(links,
                    new TypeReference<List<Map<String, Object>>>() {}));
        }
        JsonNode params = root.path("parameters");
        if (params.isArray() && params.size() > 0) {
            metadata.put("parameters", mapper.convertValue(params,
                    new TypeReference<List<Map<String, Object>>>() {}));
        }
        if (!metadata.isEmpty()) {
            req.metadata(metadata);
        }
        return req;
    }

    /**
     * Step statuses have a NARROWER server vocabulary than run statuses
     * (passed / failed / skipped / broken / error) — sending "cancelled" there
     * fails validation and rejects the whole upload. Anything unrecognised,
     * including an absent status, maps to broken: a step we never observed is
     * not a pass.
     */
    private static String mapStepStatus(String allure) {
        if (allure == null) return ExternalRunRequest.STATUS_BROKEN;
        switch (allure) {
            case "passed":  return ExternalRunRequest.STATUS_PASSED;
            case "failed":  return ExternalRunRequest.STATUS_FAILED;
            case "skipped": return ExternalRunRequest.STATUS_SKIPPED;
            default:        return ExternalRunRequest.STATUS_BROKEN;
        }
    }

    private static String mapStatus(String allure) {
        // No status at all is NOT a pass — Allure's own "unknown" and an absent
        // field both mean "we never observed an outcome", which maps to broken.
        if (allure == null || allure.isEmpty()) return ExternalRunRequest.STATUS_BROKEN;
        switch (allure) {
            case "passed":    return ExternalRunRequest.STATUS_PASSED;
            case "failed":    return ExternalRunRequest.STATUS_FAILED;
            case "broken":    return ExternalRunRequest.STATUS_BROKEN;
            case "skipped":   return ExternalRunRequest.STATUS_SKIPPED;
            case "cancelled": return ExternalRunRequest.STATUS_CANCELLED;
            default:          return ExternalRunRequest.STATUS_BROKEN;
        }
    }

    // -- streaming lifecycle -------------------------------------------------

    private String lifecycleBase(String namespace) {
        String ns = (namespace == null || namespace.isEmpty())
                ? client.getConfig().getNamespace() : namespace;
        if (ns == null || ns.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        return "/api/v1/namespaces/" + ns + "/tcm/external-runs/lifecycle";
    }

    /**
     * Opens a streaming external run and returns its server view (with the run
     * {@code id} to feed {@link #appendSteps} / {@link #finishRun}). Unlike
     * {@link #report} (one-shot upload of a finished run), the lifecycle API
     * reports incrementally: startRun → appendSteps (repeatedly) → finishRun.
     *
     * @param namespace target namespace ({@code null}/empty → client default)
     * @param run       run fields: {@code name}, {@code full_name}, {@code framework},
     *                  {@code suite_id}, {@code external_id}, {@code test_case_id},
     *                  {@code tags}, {@code environment}
     */
    public JsonNode startRun(String namespace, Map<String, Object> run) throws MockartyException {
        return client.post(lifecycleBase(namespace), run, JsonNode.class);
    }

    /** Streams one or more steps into an open run. */
    public JsonNode appendSteps(String namespace, String runId, List<Map<String, Object>> steps)
            throws MockartyException {
        return client.post(lifecycleBase(namespace) + "/" + runId + "/steps",
                Map.of("steps", steps), JsonNode.class);
    }

    /**
     * Closes an open run; the returned view carries the resolved TCM case/run
     * ids the ingest matched or created.
     */
    public JsonNode finishRun(String namespace, String runId, String status, String summary)
            throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (summary != null && !summary.isEmpty()) {
            body.put("summary", summary);
        }
        return client.post(lifecycleBase(namespace) + "/" + runId + "/finish", body, JsonNode.class);
    }

    /** Fetches the current view of a streaming run. */
    public JsonNode getRun(String namespace, String runId) throws MockartyException {
        return client.get(lifecycleBase(namespace) + "/" + runId, JsonNode.class);
    }

    /** Lists streaming runs in the namespace. */
    public List<JsonNode> listRuns(String namespace) throws MockartyException {
        JsonNode data = client.get(lifecycleBase(namespace), JsonNode.class);
        List<JsonNode> out = new ArrayList<>();
        if (data != null && data.path("runs").isArray()) {
            for (JsonNode r : data.path("runs")) {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * Flatten nested Allure step trees into ExternalStep records keyed by
     * a slash-joined path so the TCM report shows the structure. Order of
     * iteration matches the tree, which preserves the user's intent.
     */
    private static void flattenSteps(JsonNode steps, String prefix, List<ExternalStep> out) {
        Iterator<JsonNode> it = steps.elements();
        while (it.hasNext()) {
            JsonNode s = it.next();
            String name = s.path("name").asText("");
            String full = prefix.isEmpty() ? name : prefix + " / " + name;
            // A step with no status has NOT been observed to pass. Defaulting
            // it to "passed" invents a green result for work we never saw.
            ExternalStep es = new ExternalStep()
                    .name(full)
                    .status(mapStepStatus(s.path("status").asText("")));
            long st = s.path("start").asLong(0);
            long sp = s.path("stop").asLong(0);
            if (sp > st && st > 0) {
                es.durationMs(sp - st);
            }
            JsonNode sd = s.path("statusDetails");
            if (sd.isObject()) {
                String msg = sd.path("message").asText("");
                if (!msg.isEmpty()) {
                    es.error(msg);
                }
            }
            out.add(es);
            JsonNode nested = s.path("steps");
            if (nested.isArray() && nested.size() > 0) {
                flattenSteps(nested, full, out);
            }
        }
    }
}
