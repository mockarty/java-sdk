// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.api.TestRunApi;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build one release-ready report over several existing test runs.
 *
 * <p>Useful when a release gate combines heterogeneous executions (functional +
 * fuzz + chaos) into a single artifact for a dashboard, a wiki page or a CI
 * summary. Nothing is persisted server-side: the report is recomputed from the
 * listed runs on every call, so there is no parent entity to keep in sync or to
 * clean up afterwards.</p>
 *
 * <p>Configuration via environment variables: {@code MOCKARTY_SERVER},
 * {@code MOCKARTY_TOKEN}, {@code MOCKARTY_NAMESPACE}, {@code SOURCE_RUN_IDS}
 * (comma-separated), {@code REPORT_NAME} (optional), {@code OUTPUT_DIR}
 * (optional, defaults to the working directory).</p>
 */
public class AggregateTestRunsExample {

    public static void main(String[] args) throws Exception {
        String namespace = env("MOCKARTY_NAMESPACE", "default");
        String rawIds = envRequired("SOURCE_RUN_IDS").trim();
        List<String> sourceIds = Arrays.stream(rawIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (sourceIds.isEmpty()) {
            System.err.println("SOURCE_RUN_IDS must contain at least one UUID");
            System.exit(2);
        }

        // format -> file extension
        Map<String, String> formats = new LinkedHashMap<>();
        formats.put(TestRunApi.AGGREGATE_REPORT_FORMAT_UNIFIED, "json");
        formats.put(TestRunApi.AGGREGATE_REPORT_FORMAT_MARKDOWN, "md");
        formats.put(TestRunApi.AGGREGATE_REPORT_FORMAT_HTML, "html");
        formats.put(TestRunApi.AGGREGATE_REPORT_FORMAT_JUNIT, "xml");

        Path outDir = Paths.get(env("OUTPUT_DIR", "."));
        Files.createDirectories(outDir);
        String name = env("REPORT_NAME", "Release gate");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_SERVER", "http://localhost:5770"))
                .apiKey(envRequired("MOCKARTY_TOKEN"))
                .namespace(namespace)
                .timeout(Duration.ofSeconds(30))
                .build()) {

            for (Map.Entry<String, String> e : formats.entrySet()) {
                byte[] payload = client.testRuns()
                        .aggregateRunsReport(name, sourceIds, e.getKey());
                Path target = outDir.resolve("aggregate-report." + e.getValue());
                Files.write(target, payload);
                System.out.printf("%8s: %8d bytes -> %s%n",
                        e.getKey(), payload.length, target);
            }
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? fallback : v;
    }

    private static String envRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            System.err.println(key + " is required");
            System.exit(2);
        }
        return v;
    }
}
