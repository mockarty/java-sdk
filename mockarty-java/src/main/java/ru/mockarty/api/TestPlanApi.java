// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyConnectionException;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.PlanRunCancelledException;
import ru.mockarty.exception.PlanRunFailedException;
import ru.mockarty.exception.PreconditionFailedException;
import ru.mockarty.exception.WebhookDeliveryException;
import ru.mockarty.model.AdHocItem;
import ru.mockarty.model.AdHocRunResponse;
import ru.mockarty.model.AllureReport;
import ru.mockarty.model.CompareResult;
import ru.mockarty.model.UnifiedReport;
import ru.mockarty.model.CreateAdHocRunRequest;
import ru.mockarty.model.PatchOptions;
import ru.mockarty.model.PatchPlanRequest;
import ru.mockarty.model.PlanRunStatus;
import ru.mockarty.model.PlanSchedule;
import ru.mockarty.model.PlanWebhook;
import ru.mockarty.model.RunEvent;
import ru.mockarty.model.TestPlan;
import ru.mockarty.model.TestPlanRun;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * API for Test Plan management — the master orchestrator for heterogeneous
 * runs (functional / load / fuzz / chaos / contract).
 *
 * <p>Provides CRUD, run triggering, SSE subscription, report downloads,
 * schedule management and CI-integration webhooks. See
 * {@code docs/research/TEST_PLANS_ARCHITECTURE_2026-04-19.md} for the
 * end-to-end architecture.</p>
 */
public class TestPlanApi {

    private static final String BASE = "/api/v1/test-plans";

    private final MockartyClient client;

    public TestPlanApi(MockartyClient client) {
        this.client = client;
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    /** Create a new Test Plan. */
    public TestPlan create(TestPlan plan) throws MockartyException {
        if (plan.getNamespace() == null || plan.getNamespace().isEmpty()) {
            plan.namespace(client.getConfig().getNamespace());
        }
        return client.post(BASE, plan, TestPlan.class);
    }

    /** Fetch a plan by UUID or numeric id (e.g. {@code "#42"} or {@code "42"}). */
    public TestPlan get(String idOrNumeric) throws MockartyException {
        String key = normalizeId(idOrNumeric);
        return client.get(BASE + "/" + encode(key), TestPlan.class);
    }

    /** Full-replace update (PUT). */
    public TestPlan update(String planId, TestPlan plan) throws MockartyException {
        if (plan.getNamespace() == null || plan.getNamespace().isEmpty()) {
            plan.namespace(client.getConfig().getNamespace());
        }
        plan.id(planId);
        return client.put(BASE + "/" + encode(planId), plan, TestPlan.class);
    }

    /** Soft-delete a plan (sets {@code closed_at}). */
    public void delete(String planId) throws MockartyException {
        client.delete(BASE + "/" + encode(planId));
    }

    /** List plans in the caller's namespace. Pass {@code -1} for server defaults. */
    public List<TestPlan> list(String status, int limit, int offset) throws MockartyException {
        StringBuilder path = new StringBuilder(BASE).append("?namespace=")
                .append(encode(client.getConfig().getNamespace()));
        if (status != null && !status.isEmpty()) {
            path.append("&status=").append(encode(status));
        }
        if (limit > 0) {
            path.append("&limit=").append(limit);
        }
        if (offset > 0) {
            path.append("&offset=").append(offset);
        }

        JavaType envelopeType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> envelope = client.get(path.toString(), envelopeType);
        if (envelope == null) {
            return List.of();
        }
        Object items = envelope.get("items");
        if (!(items instanceof List<?>)) {
            return List.of();
        }
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, TestPlan.class);
        return client.getObjectMapper().convertValue(items, listType);
    }

    // ── Runs ─────────────────────────────────────────────────────────

    /** Trigger a plan. {@code items} / {@code mode} are nullable. */
    public TestPlanRun run(String idOrNumeric, List<Integer> items, String mode)
            throws MockartyException {
        String key = normalizeId(idOrNumeric);
        Map<String, Object> body = new LinkedHashMap<>();
        if (items != null && !items.isEmpty()) {
            body.put("items", items);
        }
        if (mode != null && !mode.isEmpty()) {
            body.put("mode", mode);
        }
        JavaType mapType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> payload = client.post(
                BASE + "/" + encode(key) + "/run",
                body.isEmpty() ? null : body,
                mapType);

        TestPlanRun run = new TestPlanRun();
        // Decode the compact response {runId, planId, status} into a TestPlanRun
        // by round-tripping via a transient map — keeps the model immutable.
        Map<String, Object> shaped = new LinkedHashMap<>();
        shaped.put("id", payload == null ? null : payload.get("runId"));
        shaped.put("planId", payload == null ? null : payload.get("planId"));
        shaped.put("status", payload == null ? null : payload.get("status"));
        return client.getObjectMapper().convertValue(shaped, TestPlanRun.class);
    }

    /** Fetch full aggregate state of a run. */
    public TestPlanRun getRun(String runId) throws MockartyException {
        return client.get(BASE + "/runs/" + encode(runId), TestPlanRun.class);
    }

    /** Fetch compact status snapshot. */
    public PlanRunStatus getRunStatus(String runId) throws MockartyException {
        return client.get(BASE + "/runs/" + encode(runId) + "/status", PlanRunStatus.class);
    }

    /** Cancel an in-flight run. Idempotent. */
    public void cancelRun(String runId) throws MockartyException {
        client.post(BASE + "/runs/" + encode(runId) + "/cancel", null);
    }

    /** List historical runs for a plan. */
    public List<TestPlanRun> listRuns(String planId, int limit, int offset)
            throws MockartyException {
        StringBuilder path = new StringBuilder(BASE).append("/")
                .append(encode(planId)).append("/runs");
        if (limit > 0 || offset > 0) {
            path.append("?");
            if (limit > 0) {
                path.append("limit=").append(limit);
            }
            if (offset > 0) {
                if (path.charAt(path.length() - 1) != '?') {
                    path.append("&");
                }
                path.append("offset=").append(offset);
            }
        }
        JavaType envelopeType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> envelope = client.get(path.toString(), envelopeType);
        if (envelope == null) {
            return List.of();
        }
        Object items = envelope.get("items");
        if (!(items instanceof List<?>)) {
            return List.of();
        }
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, TestPlanRun.class);
        return client.getObjectMapper().convertValue(items, listType);
    }

    /**
     * Diff two Test Plan runs.
     *
     * <p>Both runs MUST live in the caller's namespace (the server returns
     * 404 on cross-tenant probes — same no-leak semantics as
     * {@link #getRun(String)}). Comparing runs of different plans IS
     * allowed; {@link CompareResult.CompareSummary#isDifferentPlans()}
     * flags the case so callers can render a banner. Pass the older
     * baseline run as {@code runA} and the newer/target run as
     * {@code runB} to keep regression/improvement signs intuitive.</p>
     *
     * @param runA baseline run UUID
     * @param runB target run UUID
     * @return diff envelope with per-item rows + aggregate counters
     */
    public CompareResult compareRuns(String runA, String runB) throws MockartyException {
        if (runA == null || runA.isEmpty() || runB == null || runB.isEmpty()) {
            throw new MockartyConnectionException("runA and runB are required");
        }
        if (runA.equals(runB)) {
            throw new MockartyConnectionException("runA and runB must differ");
        }
        String path = BASE + "/runs/compare?run_a=" + encode(runA) + "&run_b=" + encode(runB);
        return client.get(path, CompareResult.class);
    }

    /**
     * Poll until the run reaches a terminal state or {@code timeout} elapses.
     * Throws {@link PlanRunFailedException} or {@link PlanRunCancelledException}
     * on the matching terminal status so CI jobs can exit non-zero.
     */
    public TestPlanRun waitForRun(String runId, Duration pollInterval, Duration timeout)
            throws MockartyException {
        long deadline = timeout == null ? Long.MAX_VALUE
                : System.nanoTime() + timeout.toNanos();
        long intervalMs = pollInterval == null ? 2000L : Math.max(1L, pollInterval.toMillis());

        while (true) {
            TestPlanRun run = getRun(runId);
            String status = run.getStatus();
            if ("completed".equals(status)) {
                return run;
            }
            if ("failed".equals(status)) {
                throw new PlanRunFailedException("run " + runId + " failed");
            }
            if ("cancelled".equals(status)) {
                throw new PlanRunCancelledException("run " + runId + " cancelled");
            }
            if (System.nanoTime() >= deadline) {
                return run;
            }
            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MockartyConnectionException("wait interrupted", e);
            }
        }
    }

    /**
     * Subscribe to the SSE stream of a run. Blocks the calling thread and
     * dispatches each frame to {@code consumer} until the server closes the
     * connection.
     */
    public void streamRun(String runId, Consumer<RunEvent> consumer) throws MockartyException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(client.getConfig().getBaseUrl() + BASE + "/runs/"
                        + encode(runId) + "/stream"))
                .header("Accept", "text/event-stream")
                .header("User-Agent", "mockarty-java-sdk")
                .header("Authorization", "Bearer " + client.getConfig().getApiKey())
                .GET()
                .build();
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(client.getConfig().getTimeout())
                .build();
        try {
            HttpResponse<java.io.InputStream> response = http.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw new MockartyException("stream failed: HTTP " + response.statusCode());
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                ObjectMapper mapper = client.getObjectMapper();
                String event = "";
                List<String> dataLines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (!event.isEmpty() || !dataLines.isEmpty()) {
                            dispatch(consumer, mapper, event, dataLines);
                        }
                        event = "";
                        dataLines.clear();
                        continue;
                    }
                    if (line.startsWith(":")) {
                        continue;
                    }
                    if (line.startsWith("event:")) {
                        event = line.substring("event:".length()).trim();
                    } else if (line.startsWith("data:")) {
                        dataLines.add(line.substring("data:".length()).trim());
                    }
                }
                // Flush a final pending frame without a trailing blank line.
                if (!event.isEmpty() || !dataLines.isEmpty()) {
                    dispatch(consumer, mapper, event, dataLines);
                }
            }
        } catch (IOException e) {
            throw new MockartyConnectionException("stream failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException("stream interrupted", e);
        }
    }

    private static void dispatch(Consumer<RunEvent> consumer,
                                 ObjectMapper mapper,
                                 String event,
                                 List<String> dataLines) {
        String raw = String.join("\n", dataLines);
        Map<String, Object> data = null;
        if (!raw.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = mapper.readValue(raw, Map.class);
                data = parsed;
            } catch (JsonProcessingException ignore) {
                // Keep raw text as the only signal.
                data = null;
            }
        }
        consumer.accept(new RunEvent(event, data, raw));
    }

    /** Download an Allure (or other format) report body as bytes. */
    public byte[] getReport(String runId, String format) throws MockartyException {
        return client.getBytes(BASE + "/runs/" + encode(runId) + "/report?format="
                + encode(format == null || format.isEmpty() ? "allure" : format));
    }

    /** Download the Allure zip archive and stream it into {@code destination}. */
    public void downloadReportZip(String runId, OutputStream destination) throws MockartyException {
        byte[] bytes = client.getBytes(BASE + "/runs/" + encode(runId) + "/report.zip");
        try {
            destination.write(bytes);
        } catch (IOException e) {
            throw new MockartyConnectionException("write zip failed: " + e.getMessage(), e);
        }
    }

    // ── Schedules ────────────────────────────────────────────────────

    public PlanSchedule addSchedule(String planId, PlanSchedule schedule) throws MockartyException {
        return client.post(BASE + "/" + encode(planId) + "/schedules", schedule, PlanSchedule.class);
    }

    public List<PlanSchedule> listSchedules(String planId) throws MockartyException {
        return listEnvelope(BASE + "/" + encode(planId) + "/schedules", PlanSchedule.class);
    }

    public PlanSchedule updateSchedule(String planId, String scheduleId, PlanSchedule schedule)
            throws MockartyException {
        return client.patch(BASE + "/" + encode(planId) + "/schedules/" + encode(scheduleId),
                schedule, PlanSchedule.class);
    }

    public void deleteSchedule(String planId, String scheduleId) throws MockartyException {
        client.delete(BASE + "/" + encode(planId) + "/schedules/" + encode(scheduleId));
    }

    // ── Webhooks ─────────────────────────────────────────────────────

    public PlanWebhook addWebhook(String planId, PlanWebhook webhook) throws MockartyException {
        return client.post(BASE + "/" + encode(planId) + "/webhooks", webhook, PlanWebhook.class);
    }

    public List<PlanWebhook> listWebhooks(String planId) throws MockartyException {
        return listEnvelope(BASE + "/" + encode(planId) + "/webhooks", PlanWebhook.class);
    }

    public PlanWebhook updateWebhook(String planId, String webhookId, PlanWebhook webhook)
            throws MockartyException {
        return client.patch(BASE + "/" + encode(planId) + "/webhooks/" + encode(webhookId),
                webhook, PlanWebhook.class);
    }

    public void deleteWebhook(String planId, String webhookId) throws MockartyException {
        client.delete(BASE + "/" + encode(planId) + "/webhooks/" + encode(webhookId));
    }

    /**
     * Server-side ping of the webhook target. Throws
     * {@link WebhookDeliveryException} when the server reports {@code success=false}.
     */
    public void testWebhook(String planId, String webhookId) throws MockartyException {
        JavaType mapType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> body = client.post(
                BASE + "/" + encode(planId) + "/webhooks/" + encode(webhookId) + "/test",
                null, mapType);
        if (body == null) {
            return;
        }
        Object success = body.get("success");
        if (Boolean.TRUE.equals(success)) {
            return;
        }
        Object err = body.get("error");
        Object status = body.get("status");
        String detail = err != null ? err.toString() : ("status=" + status);
        throw new WebhookDeliveryException("webhook " + webhookId + ": " + detail);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private <T> List<T> listEnvelope(String path, Class<T> elementType) throws MockartyException {
        JavaType envelopeType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> envelope = client.get(path, envelopeType);
        if (envelope == null) {
            return List.of();
        }
        Object items = envelope.get("items");
        if (!(items instanceof List<?>)) {
            return List.of();
        }
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, elementType);
        return client.getObjectMapper().convertValue(items, listType);
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("plan id must not be null");
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("#")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("plan id must not be empty");
        }
        return trimmed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    // ── TP-6b: namespace-scoped PATCH + ad-hoc runs + reports ────────

    /**
     * Returns {@code /api/v1/namespaces/<ns>} using the caller-supplied
     * namespace, falling back to the client default, falling back to
     * {@code sandbox}.
     */
    private String namespaceScopedBase(String explicit) {
        String ns = explicit;
        if (ns == null || ns.isEmpty()) {
            ns = client.getConfig().getNamespace();
        }
        if (ns == null || ns.isEmpty()) {
            ns = "sandbox";
        }
        return "/api/v1/namespaces/" + encode(ns);
    }

    /**
     * Apply a partial update to a Test Plan via
     * {@code PATCH /api/v1/namespaces/:namespace/test-plans/:idOrNumericID}.
     *
     * <p>The server requires an {@code If-Match} header; when
     * {@code opts.getIfMatch()} is null/empty the SDK does a pre-fetch
     * and derives the etag from the plan's {@code updatedAt}
     * timestamp — convenient for one-shot CLI flows but vulnerable to
     * lost updates in concurrent scenarios. Pass an explicit etag
     * (captured from a prior call) when correctness matters.</p>
     *
     * @throws PreconditionFailedException when the server returns
     *     {@code 412 Precondition Failed} — re-fetch, reconcile, retry.
     * @throws IllegalArgumentException when {@code req} is empty
     *     (the server rejects PATCH with no fields to change).
     */
    public TestPlan patch(String planRef, PatchPlanRequest req, PatchOptions opts)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (req == null || req.isEmpty()) {
            throw new IllegalArgumentException(
                    "patch requires at least one non-null field in PatchPlanRequest");
        }
        PatchOptions options = opts == null ? new PatchOptions() : opts;

        String ifMatch = options.getIfMatch();
        if (ifMatch == null || ifMatch.trim().isEmpty()) {
            TestPlan current = get(planRef);
            ifMatch = deriveEtag(current);
        }

        String path = namespaceScopedBase(options.getNamespace())
                + "/test-plans/" + encode(key);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(client.getConfig().getBaseUrl() + path))
                .timeout(client.getConfig().getTimeout())
                .header("User-Agent", "mockarty-java-sdk")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("If-Match", ifMatch);
        if (client.getConfig().getApiKey() != null && !client.getConfig().getApiKey().isEmpty()) {
            builder.header("Authorization", "Bearer " + client.getConfig().getApiKey());
        }

        try {
            byte[] body = client.getObjectMapper().writeValueAsBytes(req);
            builder.method("PATCH", HttpRequest.BodyPublishers.ofByteArray(body));
        } catch (JsonProcessingException e) {
            throw new MockartyException("failed to serialise PatchPlanRequest", e);
        }

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(client.getConfig().getTimeout())
                .build();

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new MockartyConnectionException("patch plan: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException("patch plan interrupted", e);
        }

        int status = response.statusCode();
        if (status == 412) {
            String etagFromServer = response.headers().firstValue("ETag").orElse(null);
            String message = truncateBody(response.body());
            throw new PreconditionFailedException(
                    "plan " + key + ": etag mismatch"
                            + (message.isEmpty() ? "" : " — " + message),
                    etagFromServer);
        }
        if (status >= 400) {
            throw new MockartyApiException(status, truncateBody(response.body()),
                    response.body());
        }
        try {
            return client.getObjectMapper().readValue(response.body(), TestPlan.class);
        } catch (JsonProcessingException e) {
            throw new MockartyException("patch plan: failed to decode response", e);
        }
    }

    /**
     * Create a hidden ad-hoc Test Plan and dispatch a master run in a
     * single call via
     * {@code POST /api/v1/namespaces/:namespace/test-runs/ad-hoc}.
     *
     * <p>The orchestrator must be enabled on the admin node — single-
     * binary / SQLite deployments without it will return
     * {@code 503 Service Unavailable}.</p>
     *
     * <p>Subsequent polling uses {@link #getRun(String)},
     * {@link #waitForRun(String, Duration, Duration)}, or
     * {@link #streamRun(String, Consumer)} against the returned
     * {@code runId}.</p>
     */
    public AdHocRunResponse createAdHocRun(CreateAdHocRunRequest req) throws MockartyException {
        if (req == null) {
            throw new IllegalArgumentException("CreateAdHocRunRequest must not be null");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("createAdHocRun requires at least one item");
        }
        for (int i = 0; i < req.getItems().size(); i++) {
            AdHocItem it = req.getItems().get(i);
            if (it == null
                    || it.getRefId() == null || it.getRefId().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "createAdHocRun: items[" + i + "].refId is required");
            }
            if (it.getType() == null || it.getType().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "createAdHocRun: items[" + i + "].type is required");
            }
        }
        String path = namespaceScopedBase(req.getNamespace()) + "/test-runs/ad-hoc";
        return client.post(path, req, AdHocRunResponse.class);
    }

    /**
     * Fetch the namespace-scoped Allure JSON report for a run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report}.
     *
     * <p>The raw bytes are preserved in {@link AllureReport#getRaw()} so
     * callers can run a second decode pass with their own types — the
     * server's schema is loosely typed on purpose (new Allure fields
     * roll out without SDK bumps).</p>
     */
    public AllureReport getRunReport(String namespace, String planRef, String runId)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report";
        byte[] raw = client.getBytes(path);
        AllureReport report;
        try {
            report = client.getObjectMapper().readValue(raw, AllureReport.class);
        } catch (IOException e) {
            // Server returned an unexpected shape — return an empty typed
            // view but keep the raw bytes so callers can reparse.
            report = new AllureReport();
        }
        return report.raw(raw);
    }

    /**
     * Stream the namespace-scoped Allure ZIP archive for a run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.zip}
     * into {@code destination}.
     *
     * <p>The caller owns {@code destination} and is responsible for
     * closing it. The SDK does <em>not</em> buffer the entire archive
     * into memory — it streams the HTTP response body directly to the
     * supplied {@link OutputStream}, so this is safe for multi-GB
     * reports.</p>
     */
    public void getRunReportZip(String namespace, String planRef, String runId,
                                OutputStream destination) throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        if (destination == null) {
            throw new IllegalArgumentException("destination OutputStream must not be null");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report.zip";

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(client.getConfig().getBaseUrl() + path))
                .timeout(client.getConfig().getTimeout())
                .header("User-Agent", "mockarty-java-sdk")
                .header("Accept", "application/zip")
                .GET();
        if (client.getConfig().getApiKey() != null && !client.getConfig().getApiKey().isEmpty()) {
            builder.header("Authorization", "Bearer " + client.getConfig().getApiKey());
        }

        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(client.getConfig().getTimeout())
                .build();

        try {
            HttpResponse<InputStream> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                String body;
                try (InputStream in = response.body()) {
                    body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                throw new MockartyApiException(response.statusCode(),
                        truncateBody(body), body);
            }
            try (InputStream in = response.body()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    destination.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            throw new MockartyConnectionException(
                    "download report zip: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MockartyConnectionException(
                    "download report zip interrupted", e);
        }
    }

    /**
     * Fetch the JUnit XML report for a namespace-scoped run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.junit.xml}.
     *
     * <p>The returned bytes are a standards-compliant JUnit document —
     * feed them straight into Jenkins {@code junit} plugin, GitLab
     * {@code reports.junit}, or the GitHub Actions JUnit reporter.</p>
     */
    public byte[] getRunReportJUnit(String namespace, String planRef, String runId)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report.junit.xml";
        return client.getBytes(path);
    }

    /**
     * Fetch the Markdown summary report for a namespace-scoped run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.md}.
     *
     * <p>Intended for Slack attachments, email bodies, wiki pastes.</p>
     */
    public byte[] getRunReportMarkdown(String namespace, String planRef, String runId)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report.md";
        return client.getBytes(path);
    }

    /**
     * Fetch the native Mockarty-shape JSON report for a namespace-scoped
     * run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.unified.json}.
     *
     * <p>The raw bytes are preserved in {@link UnifiedReport#getRaw()} so
     * callers can reparse when the server evolves the wire schema.</p>
     */
    public UnifiedReport getRunReportUnified(String namespace, String planRef, String runId)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report.unified.json";
        byte[] raw = client.getBytes(path);
        UnifiedReport report;
        try {
            report = client.getObjectMapper().readValue(raw, UnifiedReport.class);
        } catch (IOException e) {
            // Server returned an unexpected shape — return an empty typed
            // view but keep the raw bytes so callers can reparse.
            report = new UnifiedReport();
        }
        return report.raw(raw);
    }

    /**
     * Fetch the standalone, print-friendly HTML report for a
     * namespace-scoped run via
     * {@code GET /api/v1/namespaces/:ns/test-plans/:planRef/runs/:runID/report.html}.
     *
     * <p>Returns a self-contained HTML document (inlined CSS, no external
     * assets) suitable for air-gapped environments. Users can open it in
     * any browser and print to PDF via Save-as-PDF.</p>
     */
    public byte[] getRunReportHTML(String namespace, String planRef, String runId)
            throws MockartyException {
        String key = normalizeId(planRef);
        if (runId == null || runId.trim().isEmpty()) {
            throw new IllegalArgumentException("runId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/test-plans/" + encode(key)
                + "/runs/" + encode(runId.trim())
                + "/report.html";
        return client.getBytes(path);
    }

    // ── Internal helpers for TP-6b ───────────────────────────────────

    /**
     * Derives a strong-validator etag from a plan's timestamps.
     * Mirrors the Go SDK's {@code UnixMilli(UpdatedAt)} formula; falls
     * back to {@code CreatedAt} when the server didn't populate
     * {@code UpdatedAt} (fresh creates).
     */
    private static String deriveEtag(TestPlan plan) {
        String stamp = plan == null ? null : plan.getUpdatedAt();
        if (stamp == null || stamp.isEmpty()) {
            stamp = plan == null ? null : plan.getCreatedAt();
        }
        if (stamp == null || stamp.isEmpty()) {
            // Fall back to quoted empty string — server will return 412
            // rather than silently accepting an unvalidated write.
            return "\"\"";
        }
        long millis = parseIsoToMillis(stamp);
        if (millis < 0) {
            return "\"" + stamp + "\"";
        }
        return "\"" + millis + "\"";
    }

    private static long parseIsoToMillis(String iso) {
        try {
            return java.time.Instant.parse(iso).toEpochMilli();
        } catch (Exception primary) {
            try {
                return java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                return -1L;
            }
        }
    }

    private static String truncateBody(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String trimmed = body.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) + "…" : trimmed;
    }

    // ── T10: manual-flow surface ─────────────────────────────────────

    /**
     * Options for {@link #runManual(String, RunManualOptions)}. Plain
     * data-bag — null-safe getters, defaults applied at request-build time.
     */
    public static final class RunManualOptions {
        private String executionModeOverride;
        private boolean recordDetailed;
        private boolean notifyOnCompletion;
        private List<String> notifyEmails;
        private List<Integer> items;
        private String mode;

        public RunManualOptions executionModeOverride(String v) { this.executionModeOverride = v; return this; }
        public RunManualOptions recordDetailed(boolean v) { this.recordDetailed = v; return this; }
        public RunManualOptions notifyOnCompletion(boolean v) { this.notifyOnCompletion = v; return this; }
        public RunManualOptions notifyEmails(List<String> v) { this.notifyEmails = v; return this; }
        public RunManualOptions items(List<Integer> v) { this.items = v; return this; }
        public RunManualOptions mode(String v) { this.mode = v; return this; }
    }

    /**
     * Trigger a Plan run with the T6/T7/T8 manual-flow knobs.
     * <ul>
     *   <li>{@code executionModeOverride}: {@code "manual"} forces every
     *       test_case item to gate for human verdict; {@code "auto"} forces
     *       unattended; {@code null}/{@code ""} keeps the per-item default.</li>
     *   <li>{@code recordDetailed}: persist per-item HAR-shaped traces (T6).</li>
     *   <li>{@code notifyOnCompletion}/{@code notifyEmails}: completion email (T7).</li>
     * </ul>
     */
    public TestPlanRun runManual(String idOrNumeric, RunManualOptions opts)
            throws MockartyException {
        String key = normalizeId(idOrNumeric);
        RunManualOptions o = opts == null ? new RunManualOptions() : opts;
        if (o.executionModeOverride != null && !o.executionModeOverride.isEmpty()
                && !"manual".equals(o.executionModeOverride)
                && !"auto".equals(o.executionModeOverride)) {
            throw new IllegalArgumentException(
                    "executionModeOverride must be \"manual\", \"auto\", or null");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        if (o.executionModeOverride != null && !o.executionModeOverride.isEmpty()) {
            body.put("executionModeOverride", o.executionModeOverride);
        }
        if (o.recordDetailed) {
            body.put("recordDetailed", true);
        }
        if (o.notifyOnCompletion) {
            body.put("notifyOnCompletion", true);
        }
        if (o.notifyEmails != null && !o.notifyEmails.isEmpty()) {
            body.put("notifyEmails", o.notifyEmails);
        }
        if (o.items != null && !o.items.isEmpty()) {
            body.put("items", o.items);
        }
        if (o.mode != null && !o.mode.isEmpty()) {
            body.put("mode", o.mode);
        }
        JavaType mapType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> payload = client.post(
                BASE + "/" + encode(key) + "/run",
                body.isEmpty() ? null : body,
                mapType);

        Map<String, Object> shaped = new LinkedHashMap<>();
        shaped.put("id", payload == null ? null : payload.get("runId"));
        shaped.put("planId", payload == null ? null : payload.get("planId"));
        shaped.put("status", payload == null ? null : payload.get("status"));
        return client.getObjectMapper().convertValue(shaped, TestPlanRun.class);
    }

    /** Verdict pushed to a manual_pending case-run step. */
    public enum StepResolution {
        PASS("pass"), FAIL("fail"), SKIP("skip");
        private final String wire;
        StepResolution(String w) { this.wire = w; }
        public String wire() { return wire; }
    }

    /** Options for {@link #resolveStep}. */
    public static final class ResolveStepOptions {
        private StepResolution resolution;
        private String note;
        private String noteFmt;
        private List<String> attachmentIds;
        private Map<String, Object> extracted;
        private String namespace;

        public ResolveStepOptions resolution(StepResolution v) { this.resolution = v; return this; }
        public ResolveStepOptions note(String v) { this.note = v; return this; }
        public ResolveStepOptions noteFmt(String v) { this.noteFmt = v; return this; }
        public ResolveStepOptions attachmentIds(List<String> v) { this.attachmentIds = v; return this; }
        public ResolveStepOptions extracted(Map<String, Object> v) { this.extracted = v; return this; }
        public ResolveStepOptions namespace(String v) { this.namespace = v; return this; }
    }

    /**
     * Push a verdict for a manual_pending TCM case-run step.
     * Hits {@code POST /api/v1/namespaces/:ns/tcm/case-runs/:runId/steps/:stepUid/resolve}.
     *
     * <p>{@code caseRunId} is the TCM case-run UUID (NOT the plan-run id) —
     * get it from the awaiting-manual list or the parent plan-run SSE stream.</p>
     */
    public void resolveStep(String caseRunId, String stepUid, ResolveStepOptions opts)
            throws MockartyException {
        if (caseRunId == null || caseRunId.isEmpty()) {
            throw new IllegalArgumentException("caseRunId must not be empty");
        }
        if (stepUid == null || stepUid.isEmpty()) {
            throw new IllegalArgumentException("stepUid must not be empty");
        }
        ResolveStepOptions o = opts == null ? new ResolveStepOptions() : opts;
        if (o.resolution == null) {
            throw new IllegalArgumentException("resolution is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("resolution", o.resolution.wire());
        if (o.note != null) {
            body.put("note", o.note);
        }
        if (o.noteFmt != null && !o.noteFmt.isEmpty()) {
            body.put("noteFmt", o.noteFmt);
        }
        if (o.attachmentIds != null && !o.attachmentIds.isEmpty()) {
            body.put("attachments", o.attachmentIds);
        }
        if (o.extracted != null && !o.extracted.isEmpty()) {
            body.put("extracted", o.extracted);
        }
        String path = namespaceScopedBase(o.namespace)
                + "/tcm/case-runs/" + encode(caseRunId)
                + "/steps/" + encode(stepUid)
                + "/resolve";
        client.post(path, body);
    }

    private void caseRunAction(String namespace, String caseRunId, String action)
            throws MockartyException {
        if (caseRunId == null || caseRunId.isEmpty()) {
            throw new IllegalArgumentException("caseRunId must not be empty");
        }
        String path = namespaceScopedBase(namespace)
                + "/tcm/case-runs/" + encode(caseRunId)
                + "/" + action;
        client.post(path, null);
    }

    public void pauseCaseRun(String namespace, String caseRunId) throws MockartyException {
        caseRunAction(namespace, caseRunId, "pause");
    }

    public void resumeCaseRun(String namespace, String caseRunId) throws MockartyException {
        caseRunAction(namespace, caseRunId, "resume");
    }

    public void cancelCaseRun(String namespace, String caseRunId) throws MockartyException {
        caseRunAction(namespace, caseRunId, "cancel");
    }

    public void rerunCaseRun(String namespace, String caseRunId) throws MockartyException {
        caseRunAction(namespace, caseRunId, "rerun");
    }

    /** Response shape for {@code GET /api/v1/me/awaiting-manual}. */
    public Map<String, Object> awaitingManual() throws MockartyException {
        JavaType mapType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        return client.get("/api/v1/me/awaiting-manual", mapType);
    }

}
