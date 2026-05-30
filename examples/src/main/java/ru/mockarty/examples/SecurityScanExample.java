// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kick off a Mockarty Security Agent scan from CI/CD: start, poll,
 * download SARIF, gate the pipeline on critical findings.
 *
 * <p>Usage:</p>
 * <pre>
 * export MOCKARTY_BASE_URL=http://localhost:5770
 * export MOCKARTY_API_KEY=mk_xxx
 * export MOCKARTY_NAMESPACE=production
 * export SCAN_TARGET=https://api.example.com
 * mvn exec:java -pl examples -Dexec.mainClass=ru.mockarty.examples.SecurityScanExample
 * </pre>
 */
public class SecurityScanExample {

    private static final Set<String> TERMINAL = Set.of("done", "failed", "cancelled");

    public static void main(String[] args) throws Exception {
        String namespace = envOr("MOCKARTY_NAMESPACE", "sandbox");
        String target = envOr("SCAN_TARGET", "https://api.example.com");
        String sarifPath = envOr("SARIF_OUTPUT", "mockarty-security.sarif.json");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(envOr("MOCKARTY_BASE_URL", "http://localhost:5770"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .namespace(namespace)
                .build()) {

            // 1) Start a passive scan.
            Map<String, Object> report = client.security().startScan(
                    namespace, target, "web_pentester", "passive", "ci-nightly");
            String reportId = String.valueOf(report.get("id"));
            System.out.println("started: report " + reportId
                    + " (status=" + report.get("status") + ")");

            // 2) Poll until terminal — 30 min cap @ 10s intervals.
            for (int i = 0; i < 180; i++) {
                Thread.sleep(10_000);
                report = client.security().getReport(reportId);
                String status = String.valueOf(report.get("status"));
                System.out.println("  status=" + status
                        + " tokens=" + report.get("costTokens")
                        + " cost_usd_micros=" + report.get("costUsdMicros"));
                if (TERMINAL.contains(status)) {
                    break;
                }
            }
            String finalStatus = String.valueOf(report.get("status"));
            if (!"done".equals(finalStatus)) {
                System.err.println("scan ended in non-success state: " + finalStatus);
                System.exit(1);
            }

            // 3) Severities of interest.
            List<Map<String, Object>> highs =
                    client.security().listFindings(reportId, "high");
            List<Map<String, Object>> crits =
                    client.security().listFindings(reportId, "critical");
            System.out.println("findings: " + highs.size() + " high, "
                    + crits.size() + " critical");

            // 4) Persist SARIF.
            byte[] sarif = client.security().exportReport(reportId, "sarif");
            Files.write(Path.of(sarifPath), sarif);
            System.out.println("wrote " + sarifPath + " (" + sarif.length + " bytes)");

            // 5) CI gate.
            if (!crits.isEmpty()) {
                System.exit(2);
            }
        } catch (MockartyException e) {
            System.err.println("security scan failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }
}
