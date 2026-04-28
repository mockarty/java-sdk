// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.api.TestRunApi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Download the unified test-run report in every supported format for any
 * mode (functional / load / fuzz / chaos / contract / merged).
 *
 * <p>Env vars: {@code MOCKARTY_SERVER}, {@code MOCKARTY_TOKEN},
 * {@code MOCKARTY_NAMESPACE}, {@code RUN_ID}.</p>
 */
public class TestRunsReportExample {

    public static void main(String[] args) throws IOException {
        String runId = envRequired("RUN_ID");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_SERVER", "http://localhost:5770"))
                .apiKey(envRequired("MOCKARTY_TOKEN"))
                .namespace(env("MOCKARTY_NAMESPACE", "default"))
                .timeout(Duration.ofMinutes(2))
                .build()) {

            String[][] targets = new String[][]{
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_UNIFIED_JSON, "run.unified.json"},
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_ALLURE_JSON, "run.allure.json"},
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_ALLURE_ZIP, "run.allure.zip"},
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_JUNIT, "run.junit.xml"},
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_MARKDOWN, "run.md"},
                    {TestRunApi.TEST_RUN_REPORT_FORMAT_HTML, "run.html"},
            };
            for (String[] t : targets) {
                byte[] data = client.testRuns().getTestRunReport(runId, t[0]);
                Files.write(Path.of(t[1]), data);
                System.out.printf("wrote %s (%d bytes, format=%s)%n", t[1], data.length, t[0]);
            }
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private static String envRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return v;
    }
}
