// Copyright (c) 2024-2026 Mockarty. All rights reserved.
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
     * Lists all test runs.
     *
     * @return list of test runs
     */
    public List<TestRun> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, TestRun.class);
        String namespace = client.getConfig().getNamespace();
        return client.get("/api/v1/api-tester/test-runs?namespace=" + encode(namespace), listType);
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
        return client.post("/api/v1/api-tester/test-runs/import", report, TestRun.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
