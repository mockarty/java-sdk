// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.TestRun;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
