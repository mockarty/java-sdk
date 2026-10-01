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
import ru.mockarty.model.CICallback;
import ru.mockarty.model.CIJobContext;
import ru.mockarty.model.ExternalAttachment;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalRunResponse;
import ru.mockarty.model.ExternalStep;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
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
        request=CIJobContext.prepare(namespace,request,client.getObjectMapper(),System.getenv(CIJobContext.ENVIRONMENT));
        if (request.getStatus() == null || request.getStatus().isEmpty()) {
            throw new IllegalArgumentException("request.status is required");
        }
        if ((request.getCaseId() == null || request.getCaseId().isEmpty())
                && (request.getCaseName() == null || request.getCaseName().isEmpty())) {
            throw new IllegalArgumentException(
                    "one of request.caseId / request.caseName is required");
        }
        String path = "/api/v1/namespaces/" + encodePath(namespace) + "/tcm/external-runs";
        return request.getCIContext()!=null ? client.postNoRetry(path,request,ExternalRunResponse.class) : client.post(path, request, ExternalRunResponse.class);
    }

    private static String encodePath(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
    /** Finish a selected job after uploads; missing results stay a receiver-side failure. */
    public JsonNode completeGroup(String namespace,CICallback callback,String status) throws MockartyException {
        if(namespace==null||namespace.isEmpty()||callback==null)throw new IllegalArgumentException("selected CI namespace/callback required");
        if(!List.of("passed","failed","cancelled").contains(status))throw new IllegalArgumentException("invalid selected CI completion status");
        Map<String,String> wire=callback.toWire(true);
        String path="/api/v1/namespaces/"+encodePath(namespace)+"/tcm/ci-launches/"+callback.getLaunchId()+"/groups/"+callback.getGroupId()+"/complete";
        return client.postNoRetry(path,Map.of("callback",wire,"status",status),JsonNode.class);
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
        List<ExternalRunRequest> prepared=new ArrayList<>();
        boolean selected=false;
        for(ExternalRunRequest request:requests){if(request==null)throw new IllegalArgumentException("request required");ExternalRunRequest row=CIJobContext.prepare(namespace,request,client.getObjectMapper(),System.getenv(CIJobContext.ENVIRONMENT));prepared.add(row);selected|=row.getCIContext()!=null;}
        body.put("runs", prepared);
        String path = "/api/v1/namespaces/" + encodePath(namespace) + "/tcm/external-runs/batch";
        return selected ? client.postNoRetry(path,body,JsonNode.class) : client.post(path, body, JsonNode.class);
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
     * <p>Nested step media is preflighted before any POST. Limits: 256 KiB/file,
     * 1 MiB and 32 sources/result, 32 MiB/batch, 64 levels and 5000 steps/result.
     * Container fixtures are not imported.</p>
     *
     * @throws IOException                 referenced media is missing, unsafe or over its limit
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
        List<ExternalRunRequest> prepared = new ArrayList<>();
        List<Path> preparedFiles = new ArrayList<>();
        long mediaBytes = 0;
        for (Path file : files) {
            JsonNode root;
            try {
                root = mapper.readTree(AllureMedia.readSource(resultsDir, file.getFileName().toString(), 10 << 20));
            } catch (IOException e) {
                skipped.add(file.getFileName() + ": " + e.getMessage());
                continue;
            }
            ExternalRunRequest req = allureToExternalRun(root, resultsDir, mapper);
            if (req.getAttachments() != null) {
                for (ExternalAttachment attachment : req.getAttachments()) {
                    mediaBytes += java.util.Base64.getDecoder().decode(attachment.getBodyB64()).length;
                }
            }
            if (mediaBytes > (32 << 20)) throw new IOException("allure: batch attachments exceed 32 MiB");
            prepared.add(req);
            preparedFiles.add(file);
        }
        for (int index = 0; index < prepared.size(); index++) {
            Path file = preparedFiles.get(index);
            try {
                out.add(report(namespace, prepared.get(index)));
            } catch (Exception t) {
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
        AllureMedia media = AllureMedia.collect(root, resultsDir);
        if (!media.steps.isEmpty()) req.steps(media.steps);
        if (!media.attachments.isEmpty()) req.attachments(media.attachments);
        // Carry labels + parameters into metadata for richer reporting.
        Map<String, Object> metadata = new HashMap<>();
        if (!media.paths.isEmpty()) metadata.put("allureAttachmentPaths", media.paths);
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
        return client.postNoRetry(lifecycleBase(namespace), run, JsonNode.class);
    }

    /** Streams one or more steps into an open run. */
    public JsonNode appendSteps(String namespace, String runId, List<Map<String, Object>> steps)
            throws MockartyException {
        return client.postNoRetry(lifecycleRunPath(namespace, runId) + "/steps",
                Map.of("steps", steps), JsonNode.class);
    }

    /** Streams steps only while {@code revision} remains current. */
    public JsonNode appendStepsAtRevision(String namespace, String runId, long revision,
                                          List<Map<String, Object>> steps) throws MockartyException {
        return client.postWithHeadersNoRetry(lifecycleRunPath(namespace, runId) + "/steps",
                Map.of("steps", steps), JsonNode.class, revisionHeaders(revision));
    }

    /** Uploads one attachment through the legacy unfenced lane. */
    public JsonNode uploadAttachment(String namespace, String runId, String fileName, byte[] data)
            throws MockartyException {
        return client.postMultipartFileWithHeadersNoRetry(lifecycleRunPath(namespace, runId) + "/attachments",
                "file", fileName, data, JsonNode.class, Collections.emptyMap());
    }

    /** Uploads one attachment only while {@code revision} remains current. */
    public JsonNode uploadAttachmentAtRevision(String namespace, String runId, long revision,
                                                String fileName, byte[] data) throws MockartyException {
        return client.postMultipartFileWithHeadersNoRetry(lifecycleRunPath(namespace, runId) + "/attachments",
                "file", fileName, data, JsonNode.class, revisionHeaders(revision));
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
        return client.postNoRetry(lifecycleRunPath(namespace, runId) + "/finish", body, JsonNode.class);
    }

    /** Finishes only the accumulated projection identified by {@code revision}. */
    public JsonNode finishRunAtRevision(String namespace, String runId, long revision,
                                        String status, String summary) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (summary != null && !summary.isEmpty()) {
            body.put("summary", summary);
        }
        return client.postWithHeadersNoRetry(lifecycleRunPath(namespace, runId) + "/finish",
                body, JsonNode.class, revisionHeaders(revision));
    }

    /** Fetches the current view of a streaming run. */
    public JsonNode getRun(String namespace, String runId) throws MockartyException {
        return client.get(lifecycleRunPath(namespace, runId), JsonNode.class);
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

    private String lifecycleRunPath(String namespace, String runId) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId is required");
        }
        return lifecycleBase(namespace) + "/" + encode(runId);
    }

    private static Map<String, String> revisionHeaders(long revision) {
        if (revision < 1) {
            throw new IllegalArgumentException("revision must be positive");
        }
        return Map.of("If-Match", "\"" + revision + "\"");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

}
