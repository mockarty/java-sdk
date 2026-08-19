// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
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

    /** Lists test runs for a collection (client-side filter). Parity: Python list_by_collection / Go ListByCollection. */
    public List<TestRun> listByCollection(String collectionId) throws MockartyException {
        List<TestRun> out = new java.util.ArrayList<>();
        for (TestRun r : list()) {
            if (collectionId != null && collectionId.equals(r.getCollectionId())) {
                out.add(r);
            }
        }
        return out;
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

    // ── Aggregate report (stateless; replaces the removed merge surface) ──

    /**
     * Report formats for {@link #aggregateRunsReport(String, List, String)}.
     * The persistent merge surface (POST/GET/DELETE /test-runs/merges*) was
     * removed server-side in migration 100; this endpoint recomputes the
     * report per call with nothing persisted.
     */
    public static final String AGGREGATE_REPORT_FORMAT_UNIFIED = "unified";
    public static final String AGGREGATE_REPORT_FORMAT_MARKDOWN = "markdown";
    public static final String AGGREGATE_REPORT_FORMAT_HTML = "html";
    public static final String AGGREGATE_REPORT_FORMAT_JUNIT = "junit";

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
     * Builds a release-ready aggregate report over several test runs.
     *
     * <p>{@code POST /api/v1/test-runs/reports/aggregate}. Stateless — nothing
     * is persisted, each call recomputes. HTML output is self-contained
     * (inline CSS + SVG charts), so saving as PDF via the browser print dialog
     * is the supported PDF path (no server-side headless-Chrome dependency).</p>
     *
     * @param name   optional label (server falls back to "Aggregate of N runs")
     * @param runIds UUIDs of the runs to aggregate; must be non-empty
     * @param format one of {@code AGGREGATE_REPORT_FORMAT_*};
     *               {@link #AGGREGATE_REPORT_FORMAT_UNIFIED} on null/empty
     * @return raw response bytes (JSON / markdown / HTML / JUnit XML)
     */
    public byte[] aggregateRunsReport(String name, List<String> runIds, String format)
            throws MockartyException {
        if (runIds == null || runIds.isEmpty()) {
            throw new IllegalArgumentException("runIds must not be empty");
        }
        String effective = (format == null || format.isEmpty())
                ? AGGREGATE_REPORT_FORMAT_UNIFIED
                : format;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("run_ids", runIds);
        if (name != null && !name.isEmpty()) {
            body.put("name", name);
        }
        return client.postBytes(
                "/api/v1/test-runs/reports/aggregate?format=" + encode(effective), body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
