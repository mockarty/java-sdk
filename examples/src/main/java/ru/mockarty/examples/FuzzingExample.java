// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.FuzzingConfig;
import ru.mockarty.model.FuzzingFinding;
import ru.mockarty.model.FuzzingResult;
import ru.mockarty.model.FuzzingRun;
import ru.mockarty.model.FuzzingSchedule;

import java.util.List;
import java.util.Map;

/**
 * Fuzzing examples showing how to configure and run fuzz tests
 * to discover security vulnerabilities and edge cases in APIs.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Fuzzing configuration and execution</li>
 *   <li>Security-focused fuzzing (SQLi, XSS, boundary)</li>
 *   <li>Findings management (triage, analyze, replay, export)</li>
 *   <li>Import targets from cURL, OpenAPI, collections, mocks</li>
 *   <li>Scheduled fuzzing runs</li>
 *   <li>Quick fuzz without saved configs</li>
 * </ul>
 */
public class FuzzingExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createAndRunFuzzing(client);
            securityFuzzing(client);
            quickFuzzDemo(client);
            manageResults(client);
            findingsWorkflow(client);
            importTargets(client);
            scheduledFuzzing(client);
        }
    }

    /**
     * Create a fuzzing configuration and start a run.
     */
    static void createAndRunFuzzing(MockartyClient client) {
        // Define the fuzzing configuration
        FuzzingConfig config = new FuzzingConfig()
                .name("User API Fuzzing")
                .namespace("sandbox")
                .targetBaseUrl("http://localhost:8080/api/users")
                .method("POST")
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "Authorization", "Bearer test-token"
                ))
                .body(Map.of(
                        "name", "John Doe",
                        "email", "john@example.com",
                        "age", 25,
                        "role", "user"
                ))
                .fuzzFields(List.of("name", "email", "age", "role"))
                .duration(60)        // Run for 60 seconds
                .concurrency(5)      // 5 concurrent requests
                .maxRequests(1000)   // Max 1000 requests
                .securityChecks(true)
                .mutationTypes(List.of(
                        "sql_injection",
                        "xss",
                        "boundary",
                        "type_confusion",
                        "overflow"
                ));

        // Create the configuration
        FuzzingConfig created = client.fuzzing().createConfig(config);
        System.out.println("Created fuzzing config: " + created.getId());

        // Start the fuzzing run
        FuzzingRun run = client.fuzzing().start(created.getId());
        System.out.println("Started fuzzing run: " + run.getId());
        System.out.println("  Status: " + run.getStatus());

        // Wait a bit, then check results
        System.out.println("Fuzzing run started. Check results later with run ID: " + run.getId());
    }

    /**
     * Security-focused fuzzing with various attack patterns.
     */
    static void securityFuzzing(MockartyClient client) {
        // SQL Injection focused fuzzing
        FuzzingConfig sqlConfig = new FuzzingConfig()
                .name("SQL Injection Test - Login Endpoint")
                .namespace("sandbox")
                .targetBaseUrl("http://localhost:8080/api/auth/login")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(Map.of(
                        "username", "admin",
                        "password", "password123"
                ))
                .fuzzFields(List.of("username", "password"))
                .duration(30)
                .concurrency(3)
                .maxRequests(500)
                .securityChecks(true)
                .mutationTypes(List.of("sql_injection"));

        FuzzingConfig createdSql = client.fuzzing().createConfig(sqlConfig);
        System.out.println("Created SQL injection fuzzing config: " + createdSql.getId());

        // XSS focused fuzzing
        FuzzingConfig xssConfig = new FuzzingConfig()
                .name("XSS Test - Comment API")
                .namespace("sandbox")
                .targetBaseUrl("http://localhost:8080/api/comments")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(Map.of(
                        "postId", "post-123",
                        "content", "This is a comment",
                        "author", "John"
                ))
                .fuzzFields(List.of("content", "author"))
                .duration(30)
                .concurrency(3)
                .maxRequests(500)
                .securityChecks(true)
                .mutationTypes(List.of("xss"));

        FuzzingConfig createdXss = client.fuzzing().createConfig(xssConfig);
        System.out.println("Created XSS fuzzing config: " + createdXss.getId());

        // Boundary testing
        FuzzingConfig boundaryConfig = new FuzzingConfig()
                .name("Boundary Test - Pagination")
                .namespace("sandbox")
                .targetBaseUrl("http://localhost:8080/api/products?page=1&limit=10")
                .method("GET")
                .headers(Map.of("Accept", "application/json"))
                .fuzzFields(List.of("page", "limit"))
                .duration(20)
                .concurrency(2)
                .maxRequests(200)
                .mutationTypes(List.of("boundary", "overflow", "type_confusion"));

        FuzzingConfig createdBoundary = client.fuzzing().createConfig(boundaryConfig);
        System.out.println("Created boundary fuzzing config: " + createdBoundary.getId());
    }

    /**
     * Run a quick one-off fuzz test without saving a configuration.
     */
    static void quickFuzzDemo(MockartyClient client) {
        System.out.println("\n=== Quick Fuzz ===");

        FuzzingRun run = client.fuzzing().quickFuzz(Map.of(
                "url", "http://localhost:8080/api/search",
                "method", "GET"
        ));

        System.out.println("Quick fuzz started: " + run.getId());
        System.out.println("  Status: " + run.getStatus());
    }

    /**
     * Manage fuzzing results: list, inspect, summarize, and clean up.
     */
    static void manageResults(MockartyClient client) {
        System.out.println("\n=== Fuzzing Results ===");

        // List all fuzzing configs
        List<FuzzingConfig> configs = client.fuzzing().listConfigs();
        System.out.println("Total fuzzing configs: " + configs.size());
        for (FuzzingConfig cfg : configs) {
            System.out.println("  - " + cfg.getName() + " (target: " + cfg.getTargetBaseUrl() + ")");
        }

        // List all fuzzing results
        List<FuzzingResult> results = client.fuzzing().listResults();
        System.out.println("Total fuzzing results: " + results.size());

        for (FuzzingResult result : results) {
            System.out.println("  Result: " + result.getId());
            System.out.println("    Status: " + result.getStatus());
            System.out.println("    Total requests: " + result.getTotalRequests());
            System.out.println("    Total findings: " + result.getTotalFindings());
        }

        // Get details of a specific result
        if (!results.isEmpty()) {
            FuzzingResult detail = client.fuzzing().getResult(results.get(0).getId());
            System.out.println("Detailed result: " + detail);
        }

        // Get summary across all fuzzing activity
        Map<String, Object> summary = client.fuzzing().getSummary();
        System.out.println("Fuzzing summary:");
        System.out.println("  Total runs: " + summary.get("totalRuns"));
        System.out.println("  Total findings: " + summary.get("totalFindings"));
        System.out.println("  Critical: " + summary.get("criticalCount"));
    }

    /**
     * Full findings management workflow: list, triage, analyze, replay, export.
     */
    static void findingsWorkflow(MockartyClient client) {
        System.out.println("\n=== Findings Workflow ===");

        // List all findings
        List<FuzzingFinding> findings = client.fuzzing().listFindings();
        System.out.println("Total findings: " + findings.size());

        for (FuzzingFinding finding : findings) {
            System.out.println("  Finding: " + finding.getId());
            System.out.println("    Severity: " + finding.getSeverity());
            System.out.println("    Category: " + finding.getCategory());
            System.out.println("    Triaged: " + finding.getTriagedStatus());
        }

        if (findings.isEmpty()) {
            System.out.println("No findings to process.");
            return;
        }

        // Get a specific finding
        FuzzingFinding first = client.fuzzing().getFinding(findings.get(0).getId());
        System.out.println("Finding detail: " + first.getCategory() + " - " + first.getSeverity());

        // Triage a finding (mark as confirmed/false_positive/accepted)
        client.fuzzing().triageFinding(
                first.getId(),
                "confirmed",
                "Confirmed SQL injection vulnerability in login endpoint"
        );
        System.out.println("Triaged finding: " + first.getId());

        // Analyze a finding using AI
        Map<String, Object> analysis = client.fuzzing().analyzeFinding(first.getId());
        System.out.println("AI analysis: " + analysis.get("summary"));
        System.out.println("  Recommendation: " + analysis.get("recommendation"));

        // Replay a finding to reproduce the issue
        client.fuzzing().replayFinding(first.getId());
        System.out.println("Replayed finding: " + first.getId());

        // Batch operations on multiple findings
        if (findings.size() >= 2) {
            List<String> batchIds = List.of(
                    findings.get(0).getId(),
                    findings.get(1).getId()
            );

            // Batch analyze
            client.fuzzing().batchAnalyze(batchIds);
            System.out.println("Batch analyzed " + batchIds.size() + " findings");

            // Batch triage
            client.fuzzing().batchTriage(batchIds, "confirmed");
            System.out.println("Batch triaged " + batchIds.size() + " findings as confirmed");
        }

        // Export findings
        byte[] exportData = client.fuzzing().exportFindings(Map.of(
                "format", "json",
                "severity", List.of("critical", "high")
        ));
        System.out.println("Exported findings: " + exportData.length + " bytes");
    }

    /**
     * Import fuzzing targets from various sources.
     */
    static void importTargets(MockartyClient client) {
        System.out.println("\n=== Import Fuzzing Targets ===");

        // Import from cURL command
        client.fuzzing().importFromCurl(
                "curl -X POST http://localhost:8080/api/users " +
                "-H 'Content-Type: application/json' " +
                "-d '{\"name\": \"test\", \"email\": \"test@example.com\"}'"
        );
        System.out.println("Imported target from cURL");

        // Import from OpenAPI specification
        client.fuzzing().importFromOpenAPI(Map.of(
                "spec", "openapi: '3.0.0'\ninfo:\n  title: Test API\n  version: '1.0.0'",
                "namespace", "sandbox"
        ));
        System.out.println("Imported targets from OpenAPI");

        // Import from a test collection
        client.fuzzing().importFromCollection(Map.of(
                "collectionId", "my-test-collection",
                "namespace", "sandbox"
        ));
        System.out.println("Imported targets from collection");

        // Import from a recorder session
        client.fuzzing().importFromRecorder(Map.of(
                "sessionId", "recorder-session-1",
                "namespace", "sandbox"
        ));
        System.out.println("Imported targets from recorder");

        // Import from an existing mock
        client.fuzzing().importFromMock(Map.of(
                "mockId", "http-get-user",
                "namespace", "sandbox"
        ));
        System.out.println("Imported target from mock");
    }

    /**
     * Create and manage scheduled fuzzing runs.
     */
    static void scheduledFuzzing(MockartyClient client) {
        System.out.println("\n=== Scheduled Fuzzing ===");

        // List existing configs first
        List<FuzzingConfig> configs = client.fuzzing().listConfigs();
        if (configs.isEmpty()) {
            System.out.println("No configs available for scheduling");
            return;
        }

        String configId = configs.get(0).getId();

        // Create a fuzzing schedule (run nightly)
        FuzzingSchedule schedule = new FuzzingSchedule()
                .name("Nightly Security Scan")
                .configId(configId)
                .cronExpression("0 2 * * *")    // Every day at 2 AM
                .enabled(true)
                .notifyOnFailure(true);

        FuzzingSchedule created = client.fuzzing().createSchedule(schedule);
        System.out.println("Created schedule: " + created.getId());
        System.out.println("  Name: " + created.getName());
        System.out.println("  Cron: " + created.getCronExpression());

        // List all schedules
        List<FuzzingSchedule> schedules = client.fuzzing().listSchedules();
        System.out.println("Total schedules: " + schedules.size());
        for (FuzzingSchedule s : schedules) {
            System.out.println("  " + s.getName() + " (enabled=" + s.getEnabled() + ")");
        }

        // Update a schedule (change cron, disable)
        FuzzingSchedule updated = client.fuzzing().updateSchedule(created.getId(),
                new FuzzingSchedule()
                        .name("Weekly Security Scan")
                        .configId(configId)
                        .cronExpression("0 2 * * 0")    // Every Sunday at 2 AM
                        .enabled(false)
                        .notifyOnFailure(true)
        );
        System.out.println("Updated schedule: " + updated.getName());

        // Delete a schedule
        // client.fuzzing().deleteSchedule(created.getId());
        // System.out.println("Deleted schedule");
    }
}
