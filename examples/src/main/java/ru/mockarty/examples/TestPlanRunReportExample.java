// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.AllureReport;
import ru.mockarty.model.UnifiedReport;
import ru.mockarty.model.UnifiedReportCounts;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Download every available report format for a Test Plan run.
 *
 * <p>Produces five files on disk:</p>
 * <ul>
 *   <li>{@code report.json}          — Allure-compatible JSON</li>
 *   <li>{@code report.zip}           — Allure directory as a zip</li>
 *   <li>{@code report.junit.xml}     — Standards-compliant JUnit XML</li>
 *   <li>{@code report.md}            — Markdown human summary</li>
 *   <li>{@code report.unified.json}  — Native Mockarty unified envelope</li>
 * </ul>
 *
 * <p>Configuration via environment variables: {@code MOCKARTY_SERVER},
 * {@code MOCKARTY_TOKEN}, {@code MOCKARTY_NAMESPACE}, {@code PLAN_REF},
 * {@code RUN_ID}.</p>
 */
public class TestPlanRunReportExample {

    public static void main(String[] args) throws IOException {
        String namespace = env("MOCKARTY_NAMESPACE", "default");
        String planRef = envRequired("PLAN_REF");
        String runId = envRequired("RUN_ID");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_SERVER", "http://localhost:5770"))
                .apiKey(envRequired("MOCKARTY_TOKEN"))
                .namespace(namespace)
                .timeout(Duration.ofMinutes(2))
                .build()) {

            // 1. Allure JSON — strongly typed decode + raw bytes.
            AllureReport allure = client.testPlans()
                    .getRunReport(namespace, planRef, runId);
            Files.write(Path.of("report.json"),
                    allure.getRaw() == null ? new byte[0] : allure.getRaw());
            System.out.println("wrote report.json — status=" + allure.getStatus());

            // 2. Allure ZIP — streamed chunk-by-chunk.
            try (FileOutputStream out = new FileOutputStream("report.zip")) {
                client.testPlans()
                        .getRunReportZip(namespace, planRef, runId, out);
            }
            System.out.println("wrote report.zip");

            // 3. JUnit XML — feed into Jenkins / GitLab / GitHub Actions.
            byte[] junit = client.testPlans()
                    .getRunReportJUnit(namespace, planRef, runId);
            Files.write(Path.of("report.junit.xml"), junit);
            System.out.println("wrote report.junit.xml (" + junit.length + " bytes)");

            // 4. Markdown — Slack / email / wiki ready.
            byte[] md = client.testPlans()
                    .getRunReportMarkdown(namespace, planRef, runId);
            Files.write(Path.of("report.md"), md);
            System.out.println("wrote report.md (" + md.length + " bytes)");

            // 5. Unified JSON — native envelope, typed counts.
            UnifiedReport unified = client.testPlans()
                    .getRunReportUnified(namespace, planRef, runId);
            Files.write(Path.of("report.unified.json"),
                    unified.getRaw() == null ? new byte[0] : unified.getRaw());
            UnifiedReportCounts c = unified.getCounts();
            System.out.printf(
                    "wrote report.unified.json — plan=%s run=%s items=%d " +
                    "(passed=%d failed=%d skipped=%d broken=%d) duration=%sms%n",
                    unified.getPlanName(), unified.getRunId(),
                    c == null ? 0 : c.getTotal(),
                    c == null ? 0 : c.getPassed(),
                    c == null ? 0 : c.getFailed(),
                    c == null ? 0 : c.getSkipped(),
                    c == null ? 0 : c.getBroken(),
                    unified.getDurationMs());
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v == null || v.isEmpty()) ? fallback : v;
    }

    private static String envRequired(String key) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException(key + " is required");
        }
        return v;
    }
}
