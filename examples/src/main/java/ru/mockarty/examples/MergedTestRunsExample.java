// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.api.TestRunApi;
import ru.mockarty.model.MergedRunList;
import ru.mockarty.model.MergedRunView;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Aggregate several existing test runs into a single merged run.
 *
 * <p>Useful when a release gate combines heterogeneous executions (functional +
 * fuzz + chaos) into one artifact. The merge parent row tracks the sources
 * live; downloading the unified JSON report gives a language-neutral envelope,
 * markdown is Slack-ready.</p>
 *
 * <p>Configuration via environment variables: {@code MOCKARTY_SERVER},
 * {@code MOCKARTY_TOKEN}, {@code MOCKARTY_NAMESPACE}, {@code SOURCE_RUN_IDS}
 * (comma-separated), {@code MERGE_NAME} (optional), {@code DELETE_AFTER}
 * (set to {@code 1} to cleanup).</p>
 */
public class MergedTestRunsExample {

    public static void main(String[] args) {
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

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_SERVER", "http://localhost:5770"))
                .apiKey(envRequired("MOCKARTY_TOKEN"))
                .namespace(namespace)
                .timeout(Duration.ofSeconds(30))
                .build()) {

            String name = env("MERGE_NAME", "Release gate");
            MergedRunView view = client.testRuns().mergeRuns(name, sourceIds);
            String mergedId = view.getRun() != null ? view.getRun().getId() : "<unknown>";
            System.out.printf("Created merged run %s with %d sources%n",
                    mergedId, view.getSources().size());

            // Latest snapshot — terminal-transition hook may have already
            // rolled up final totals if sources were already terminal.
            MergedRunView latest = client.testRuns().getMergedRun(mergedId);
            System.out.printf("Status: %s%n",
                    latest.getRun() != null ? latest.getRun().getStatus() : "unknown");

            // List merges in the namespace.
            MergedRunList page = client.testRuns().listMergedRuns(10, 0);
            System.out.printf("Namespace has %d merged runs (page size %d)%n",
                    page.getTotal(), page.getLimit());

            // Download both report formats.
            byte[] unified = client.testRuns().getMergedRunReport(
                    mergedId, TestRunApi.MERGED_RUN_REPORT_FORMAT_UNIFIED);
            byte[] markdown = client.testRuns().getMergedRunReport(
                    mergedId, TestRunApi.MERGED_RUN_REPORT_FORMAT_MARKDOWN);
            System.out.printf("Unified report: %d bytes; markdown: %d bytes%n",
                    unified.length, markdown.length);

            if ("1".equals(System.getenv("DELETE_AFTER"))) {
                client.testRuns().deleteMergedRun(mergedId);
                System.out.println("Deleted merged run " + mergedId);
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
