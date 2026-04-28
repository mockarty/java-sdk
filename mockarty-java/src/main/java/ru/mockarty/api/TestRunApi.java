// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.MergedRunList;
import ru.mockarty.model.MergedRunView;
import ru.mockarty.model.TestRun;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * API for test run management and reporting.
 */
public class TestRunApi {

    private final MockartyClient client;

    public TestRunApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all test runs (functional mode by default).
     *
     * @return list of test runs
     */
    public List<TestRun> list() throws MockartyException {
        return listByMode(null, null, 0, 0);
    }

    /**
     * Lists test runs filtered by execution mode (migration 033). Pass
     * {@code mode="fuzz"} / {@code "chaos"} / {@code "contract"} / {@code "load"}
     * to see runs from those subsystems; {@code null} returns the default
     * functional view. {@code referenceId} narrows to one owning row.
     *
     * @param mode        execution mode filter (nullable)
     * @param referenceId subsystem-owned row id filter (nullable)
     * @param limit       page size (&lt;=0 → server default)
     * @param offset      page offset (&lt;=0 → none)
     * @return list of test runs
     */
    public List<TestRun> listByMode(String mode, String referenceId, int limit, int offset)
            throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/api-tester/test-runs?namespace=");
        path.append(encode(client.getConfig().getNamespace()));
        if (mode != null && !mode.isEmpty()) {
            path.append("&mode=").append(encode(mode));
        }
        if (referenceId != null && !referenceId.isEmpty()) {
            path.append("&referenceId=").append(encode(referenceId));
        }
        if (limit > 0) {
            path.append("&limit=").append(limit);
        }
        if (offset > 0) {
            path.append("&offset=").append(offset);
        }
        // Server returns either a bare list (legacy) or an envelope {runs:[...]};
        // try the envelope shape first, then fall back to list.
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = client.get(path.toString(), Map.class);
            if (envelope != null && envelope.get("runs") instanceof List<?>) {
                JavaType trType = client.getObjectMapper().getTypeFactory()
                        .constructCollectionType(List.class, TestRun.class);
                return client.getObjectMapper().convertValue(envelope.get("runs"), trType);
            }
        } catch (MockartyException ignore) {
            // fall through to list decode
        }
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, TestRun.class);
        return client.get(path.toString(), listType);
    }

    /**
     * Gets a specific test run by ID.
     *
     * @param id the test run ID
     * @return the test run
     */
    public TestRun get(String id) throws MockartyException {
        return client.get("/api/v1/api-tester/test-runs/" + encode(id), TestRun.class);
    }

    /**
     * Cancels a running test.
     *
     * @param id the test run ID to cancel
     */
    public void cancel(String id) throws MockartyException {
        client.post("/api/v1/api-tester/test-runs/" + encode(id) + "/cancel", null);
    }

    /**
     * Deletes a test run and its results.
     *
     * @param id the test run ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/api-tester/test-runs/" + encode(id));
    }

    /**
     * Exports a test run report as bytes (e.g., JSON or PDF).
     *
     * @param id     the test run ID
     * @param format the export format ("json" or "pdf")
     * @return the exported report bytes
     */
    public byte[] export(String id, String format) throws MockartyException {
        return client.getBytes("/api/v1/api-tester/test-runs/" + encode(id) + "/export?format=" + encode(format));
    }

    /**
     * Imports a test run report.
     *
     * @param report the report data to import
     * @return the imported test run
     */
    public TestRun importReport(Map<String, Object> report) throws MockartyException {
        return client.post("/api/v1/api-tester/reports/import", report, TestRun.class);
    }

    /**
     * Lists active (pending/running) test runs in the current namespace.
     * Useful for CI/CD gating on parallel runs.
     *
     * @return list of active test runs
     */
    @SuppressWarnings("unchecked")
    public List<TestRun> listActive() throws MockartyException {
        Map<String, Object> envelope = client.get("/api/v1/test-runs/active", Map.class);
        if (envelope == null) {
            return List.of();
        }
        Object raw = envelope.get("runs");
        if (!(raw instanceof List<?>)) {
            return List.of();
        }
        JavaType trType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, TestRun.class);
        return client.getObjectMapper().convertValue(raw, trType);
    }

    // ── Merged test runs (T-12 / backlog #55) ──────────────────────────

    /**
     * Report formats accepted by {@link #getMergedRunReport(String, String)}.
     * Allure/JUnit/HTML are intentionally unsupported: merged runs span
     * heterogeneous sources and have no plan/DAG shape to project into
     * Allure's test-suite semantics.
     */
    public static final String MERGED_RUN_REPORT_FORMAT_UNIFIED = "unified";
    public static final String MERGED_RUN_REPORT_FORMAT_MARKDOWN = "markdown";

    /**
     * Report formats accepted by {@link #getTestRunReport(String, String)}
     * (backlog #67 unified per-run endpoint). Supports every execution mode
     * (functional / load / fuzz / chaos / contract / merged); fuzz findings,
     * chaos fault outcomes and contract case results expand into per-item
     * AllureResult rows.
     */
    public static final String TEST_RUN_REPORT_FORMAT_ALLURE_ZIP = "allure_zip";
    public static final String TEST_RUN_REPORT_FORMAT_ALLURE_JSON = "allure_json";
    public static final String TEST_RUN_REPORT_FORMAT_JUNIT = "junit";
    public static final String TEST_RUN_REPORT_FORMAT_MARKDOWN = "markdown";
    public static final String TEST_RUN_REPORT_FORMAT_UNIFIED_JSON = "unified_json";
    public static final String TEST_RUN_REPORT_FORMAT_HTML = "html";

    /**
     * Downloads the aggregated report for a single test run.
     *
     * @param runId  UUID of the run (any mode)
     * @param format one of {@code TEST_RUN_REPORT_FORMAT_*}; defaults to
     *               {@link #TEST_RUN_REPORT_FORMAT_UNIFIED_JSON} on null/empty
     * @return raw response bytes (zip / JSON / XML / markdown / HTML text)
     */
    public byte[] getTestRunReport(String runId, String format) throws MockartyException {
        String effective = (format == null || format.isEmpty())
                ? TEST_RUN_REPORT_FORMAT_UNIFIED_JSON
                : format;
        return client.getBytes(
                "/api/v1/api-tester/test-runs/" + encode(runId)
                        + "/report?format=" + encode(effective));
    }

    /**
     * Creates a merged test run aggregating {@code sourceRunIds}.
     *
     * <p>Equivalent to {@code POST /api/v1/test-runs/merges}.
     * {@code sourceRunIds} must contain at least one UUID; the server enforces
     * cross-namespace rules (admin/support bypass).</p>
     *
     * @param name         human-readable label for the merge
     * @param sourceRunIds UUIDs of existing runs to attach
     * @return the freshly-created parent row with the initial source snapshot
     */
    public MergedRunView mergeRuns(String name, List<String> sourceRunIds) throws MockartyException {
        if (sourceRunIds == null || sourceRunIds.isEmpty()) {
            throw new IllegalArgumentException("sourceRunIds must not be empty");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name == null ? "" : name);
        body.put("sourceRunIds", sourceRunIds);
        return client.post("/api/v1/test-runs/merges", body, MergedRunView.class);
    }

    /** Convenience wrapper with server-default pagination. */
    public MergedRunList listMergedRuns() throws MockartyException {
        return listMergedRuns(0, 0);
    }

    /**
     * Lists merged runs in the client's namespace, newest first.
     *
     * @param limit  page size; &lt;=0 uses the server default (50). Capped at 500.
     * @param offset page offset; &lt;=0 means no offset.
     * @return paginated envelope (items + total/limit/offset)
     */
    public MergedRunList listMergedRuns(int limit, int offset) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/test-runs/merges");
        boolean first = true;
        if (limit > 0) {
            path.append(first ? '?' : '&').append("limit=").append(limit);
            first = false;
        }
        if (offset > 0) {
            path.append(first ? '?' : '&').append("offset=").append(offset);
        }
        return client.get(path.toString(), MergedRunList.class);
    }

    /**
     * Fetches a merged run with the latest source snapshot.
     *
     * @param mergedRunId UUID of the merged parent row
     */
    public MergedRunView getMergedRun(String mergedRunId) throws MockartyException {
        return client.get(
                "/api/v1/test-runs/merges/" + encode(mergedRunId),
                MergedRunView.class);
    }

    /**
     * Deletes the merge parent. Source runs are untouched; edge rows in
     * {@code test_run_merges} are dropped by ON DELETE CASCADE.
     */
    public void deleteMergedRun(String mergedRunId) throws MockartyException {
        client.delete("/api/v1/test-runs/merges/" + encode(mergedRunId));
    }

    /**
     * Downloads the aggregated merged-run report.
     *
     * @param mergedRunId UUID of the merged parent row
     * @param format      {@link #MERGED_RUN_REPORT_FORMAT_UNIFIED} (default on null/empty)
     *                    or {@link #MERGED_RUN_REPORT_FORMAT_MARKDOWN}
     * @return raw response bytes (JSON or markdown text)
     */
    public byte[] getMergedRunReport(String mergedRunId, String format) throws MockartyException {
        String effective = (format == null || format.isEmpty())
                ? MERGED_RUN_REPORT_FORMAT_UNIFIED
                : format;
        return client.getBytes(
                "/api/v1/test-runs/merges/" + encode(mergedRunId)
                        + "/report?format=" + encode(effective));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
