// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.model.CanIDeployResult;
import ru.mockarty.model.FuzzingConfig;
import ru.mockarty.model.FuzzingFinding;
import ru.mockarty.model.FuzzingResult;
import ru.mockarty.model.FuzzingRun;
import ru.mockarty.model.GeneratorRequest;
import ru.mockarty.model.GeneratorResponse;
import ru.mockarty.model.ImportResult;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;
import ru.mockarty.model.Pact;
import ru.mockarty.model.PactVerificationResult;
import ru.mockarty.model.PerfConfig;
import ru.mockarty.model.TestRun;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Complete CI/CD pipeline automation example demonstrating end-to-end
 * testing workflow using the Mockarty Java SDK.
 *
 * <p>This example simulates a full pipeline that:</p>
 * <ol>
 *   <li>Sets up a dedicated namespace for the pipeline run</li>
 *   <li>Imports an OpenAPI spec and generates mocks</li>
 *   <li>Validates contracts using pacts</li>
 *   <li>Executes a test collection against the mocks</li>
 *   <li>Runs security fuzzing</li>
 *   <li>Executes a performance test</li>
 *   <li>Collects results and exports reports</li>
 *   <li>Cleans up all resources</li>
 * </ol>
 *
 * <p>Designed for use in Jenkins, GitHub Actions, GitLab CI, or any CI/CD system.</p>
 */
public class CiCdPipelineExample {

    private static final String PIPELINE_NAMESPACE = "ci-pipeline";
    private static final String BUILD_ID = System.getenv().getOrDefault("BUILD_ID", "local-dev");

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("MOCKARTY_BASE_URL", "http://localhost:5770");
        String apiKey = System.getenv().getOrDefault("MOCKARTY_API_KEY", "your-api-key");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .namespace(PIPELINE_NAMESPACE)
                .build()) {

            PipelineResult result = runPipeline(client);
            printReport(result);

            // Exit with appropriate code for CI/CD
            if (!result.passed) {
                System.exit(1);
            }
        }
    }

    /**
     * Execute the full CI/CD pipeline.
     */
    static PipelineResult runPipeline(MockartyClient client) {
        PipelineResult result = new PipelineResult();
        result.buildId = BUILD_ID;

        try {
            // Step 1: Setup namespace
            step1_setupNamespace(client, result);

            // Step 2: Import OpenAPI and generate mocks
            step2_importAndGenerateMocks(client, result);

            // Step 3: Contract validation with pacts
            step3_contractValidation(client, result);

            // Step 4: Execute test collection
            step4_executeTests(client, result);

            // Step 5: Security fuzzing
            step5_fuzzing(client, result);

            // Step 6: Performance testing
            step6_performanceTest(client, result);

            // Step 7: Collect results and export
            step7_collectResults(client, result);

        } catch (Exception e) {
            result.passed = false;
            result.errors.add("Pipeline failed: " + e.getMessage());
            System.err.println("Pipeline error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Step 8: Cleanup
            step8_cleanup(client, result);
        }

        return result;
    }

    // ---- Pipeline Steps ----

    /**
     * Step 1: Create a dedicated namespace for this pipeline run.
     */
    static void step1_setupNamespace(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 1: Setup Namespace ===");

        try {
            client.namespaces().create(PIPELINE_NAMESPACE);
            System.out.println("Created namespace: " + PIPELINE_NAMESPACE);
        } catch (MockartyApiException e) {
            // Namespace may already exist
            System.out.println("Namespace exists or created: " + PIPELINE_NAMESPACE);
        }

        // Verify health
        boolean ready = client.health().ready();
        if (!ready) {
            throw new RuntimeException("Mockarty server is not ready");
        }
        System.out.println("Server is healthy and ready");

        // Check system status
        Map<String, Object> status = client.stats().getStatus();
        System.out.println("Server version: " + status.get("version"));

        result.steps.add("setup: OK");
    }

    /**
     * Step 2: Import OpenAPI specification and generate mocks.
     */
    static void step2_importAndGenerateMocks(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 2: Import OpenAPI & Generate Mocks ===");

        String openApiSpec = """
                openapi: '3.0.0'
                info:
                  title: User Service API
                  version: '2.0.0'
                paths:
                  /api/users:
                    get:
                      summary: List users
                      parameters:
                        - name: limit
                          in: query
                          schema:
                            type: integer
                            default: 10
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  $ref: '#/components/schemas/User'
                    post:
                      summary: Create user
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/CreateUser'
                      responses:
                        '201':
                          description: Created
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/User'
                        '422':
                          description: Validation error
                  /api/users/{id}:
                    get:
                      summary: Get user
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/User'
                        '404':
                          description: Not found
                    put:
                      summary: Update user
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/CreateUser'
                      responses:
                        '200':
                          description: Updated
                    delete:
                      summary: Delete user
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                      responses:
                        '204':
                          description: Deleted
                components:
                  schemas:
                    User:
                      type: object
                      required: [id, name, email]
                      properties:
                        id:
                          type: string
                        name:
                          type: string
                        email:
                          type: string
                          format: email
                        role:
                          type: string
                          enum: [admin, user, moderator]
                    CreateUser:
                      type: object
                      required: [name, email]
                      properties:
                        name:
                          type: string
                        email:
                          type: string
                          format: email
                        role:
                          type: string
                """;

        // Generate mocks from the spec
        GeneratorResponse genResult = client.generator().fromOpenAPI(
                new GeneratorRequest()
                        .spec(openApiSpec)
                        .namespace(PIPELINE_NAMESPACE)
                        .generateFaker(true)
        );
        result.mocksGenerated = genResult.getMocksCreated();
        System.out.println("Generated " + genResult.getMocksCreated() + " mocks from OpenAPI spec");

        // Verify mocks were created
        Page<Mock> mocks = client.mocks().list(PIPELINE_NAMESPACE, null, null, 0, 50);
        System.out.println("Total mocks in namespace: " + mocks.getTotal());

        result.steps.add("import: " + genResult.getMocksCreated() + " mocks generated");
    }

    /**
     * Step 3: Validate contracts using pact-based testing.
     */
    static void step3_contractValidation(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 3: Contract Validation (Pacts) ===");

        // Publish a consumer pact
        Pact pact = new Pact()
                .consumer("order-service")
                .provider("user-service")
                .version(BUILD_ID)
                .interactions(List.of(
                        Map.of(
                                "description", "get user for order processing",
                                "request", Map.of(
                                        "method", "GET",
                                        "path", "/api/users/user-123"
                                ),
                                "response", Map.of(
                                        "status", 200,
                                        "body", Map.of(
                                                "id", "user-123",
                                                "name", "John Doe",
                                                "email", "john@example.com"
                                        )
                                )
                        )
                ));

        Pact published = client.contracts().publishPact(pact);
        System.out.println("Published pact: " + published.getId());

        // Verify the pact against the mock provider
        PactVerificationResult verification = client.contracts().verifyPact(Map.of(
                "pactId", published.getId(),
                "providerUrl", client.getConfig().getBaseUrl(),
                "providerVersion", BUILD_ID
        ));

        result.contractsPassed = verification.isSuccess();
        System.out.println("Pact verification: " + (verification.isSuccess() ? "PASSED" : "FAILED"));

        // Can I deploy check
        CanIDeployResult deployCheck = client.contracts().canIDeploy(Map.of(
                "application", "order-service",
                "version", BUILD_ID,
                "to", "staging"
        ));

        result.canDeploy = deployCheck.isDeployable();
        System.out.println("Can I deploy to staging? " + deployCheck.isDeployable());

        result.steps.add("contracts: " + (verification.isSuccess() ? "PASSED" : "FAILED"));
    }

    /**
     * Step 4: Execute a test collection against the mocks.
     */
    static void step4_executeTests(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 4: Execute Test Collection ===");

        // Create a test collection
        Map<String, Object> collection = client.collections().create(Map.of(
                "name", "CI Pipeline Tests - " + BUILD_ID,
                "namespace", PIPELINE_NAMESPACE,
                "requests", List.of(
                        Map.of(
                                "name", "List Users",
                                "method", "GET",
                                "url", "{{baseUrl}}/api/users",
                                "assertions", List.of(
                                        Map.of("type", "status", "expected", 200)
                                )
                        ),
                        Map.of(
                                "name", "Create User",
                                "method", "POST",
                                "url", "{{baseUrl}}/api/users",
                                "body", Map.of("name", "Test User", "email", "test@example.com"),
                                "assertions", List.of(
                                        Map.of("type", "status", "expected", 201)
                                )
                        ),
                        Map.of(
                                "name", "Get User",
                                "method", "GET",
                                "url", "{{baseUrl}}/api/users/test-user",
                                "assertions", List.of(
                                        Map.of("type", "status", "expected", 200),
                                        Map.of("type", "jsonpath", "path", "$.id", "expected", "test-user")
                                )
                        )
                )
        ));

        String collectionId = collection.get("id").toString();
        System.out.println("Created test collection: " + collectionId);

        // Run the collection
        Map<String, Object> runResult = client.collections().run(collectionId);
        System.out.println("Test run completed:");
        System.out.println("  Total: " + runResult.get("total"));
        System.out.println("  Passed: " + runResult.get("passed"));
        System.out.println("  Failed: " + runResult.get("failed"));

        Object failedCount = runResult.get("failed");
        result.testsPassed = failedCount instanceof Number && ((Number) failedCount).intValue() == 0;
        result.testsTotal = runResult.get("total") instanceof Number ?
                ((Number) runResult.get("total")).intValue() : 0;
        result.testsFailed = failedCount instanceof Number ?
                ((Number) failedCount).intValue() : 0;

        result.steps.add("tests: " + result.testsTotal + " total, " +
                result.testsFailed + " failed");
    }

    /**
     * Step 5: Run security fuzzing against the API.
     */
    static void step5_fuzzing(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 5: Security Fuzzing ===");

        // Create a fuzzing config
        FuzzingConfig config = new FuzzingConfig()
                .name("CI Pipeline Fuzzing - " + BUILD_ID)
                .namespace(PIPELINE_NAMESPACE)
                .targetUrl(client.getConfig().getBaseUrl() + "/api/users")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body(Map.of(
                        "name", "test",
                        "email", "test@test.com"
                ))
                .fuzzFields(List.of("name", "email"))
                .duration(30)          // Short run for CI
                .concurrency(3)
                .maxRequests(200)
                .securityChecks(true)
                .mutationTypes(List.of("sql_injection", "xss", "boundary"));

        FuzzingConfig created = client.fuzzing().createConfig(config);

        // Start fuzzing
        FuzzingRun run = client.fuzzing().start(created.getId());
        System.out.println("Started fuzzing run: " + run.getId());

        // Wait for completion (in real CI, poll with timeout)
        // For this example, we check results immediately
        List<FuzzingResult> results = client.fuzzing().listResults();
        System.out.println("Fuzzing results: " + results.size());

        // Check for critical findings
        List<FuzzingFinding> findings = client.fuzzing().listFindings();
        long criticalCount = findings.stream()
                .filter(f -> "critical".equals(f.getSeverity()) || "high".equals(f.getSeverity()))
                .count();

        result.fuzzingFindings = findings.size();
        result.fuzzingCritical = (int) criticalCount;
        result.fuzzingPassed = criticalCount == 0;

        System.out.println("Fuzzing findings: " + findings.size() +
                " (critical/high: " + criticalCount + ")");

        result.steps.add("fuzzing: " + findings.size() + " findings (" +
                criticalCount + " critical/high)");
    }

    /**
     * Step 6: Run a performance test.
     */
    static void step6_performanceTest(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 6: Performance Test ===");

        // Create a perf test config
        PerfConfig perfConfig = new PerfConfig()
                .name("CI Pipeline Load Test - " + BUILD_ID)
                .namespace(PIPELINE_NAMESPACE);

        PerfConfig createdConfig = client.perf().createConfig(perfConfig);

        // Run a quick load test
        Map<String, Object> perfResult = client.perf().run(Map.of(
                "configId", createdConfig.getId(),
                "targetUrl", client.getConfig().getBaseUrl() + "/api/users",
                "method", "GET",
                "duration", 15,         // 15 seconds
                "concurrency", 10,
                "rps", 50               // 50 requests per second
        ));

        System.out.println("Performance test completed:");
        System.out.println("  Total requests: " + perfResult.get("totalRequests"));
        System.out.println("  Avg latency: " + perfResult.get("avgLatencyMs") + "ms");
        System.out.println("  P95 latency: " + perfResult.get("p95LatencyMs") + "ms");
        System.out.println("  P99 latency: " + perfResult.get("p99LatencyMs") + "ms");
        System.out.println("  Error rate: " + perfResult.get("errorRate") + "%");

        // Check if performance meets SLA
        Object p95 = perfResult.get("p95LatencyMs");
        Object errorRate = perfResult.get("errorRate");
        boolean meetsLatency = p95 instanceof Number && ((Number) p95).doubleValue() < 500;
        boolean meetsErrors = errorRate instanceof Number && ((Number) errorRate).doubleValue() < 1.0;

        result.perfPassed = meetsLatency && meetsErrors;
        System.out.println("Performance SLA met: " + result.perfPassed);

        result.steps.add("perf: " + (result.perfPassed ? "PASSED" : "FAILED") +
                " (p95=" + p95 + "ms, errors=" + errorRate + "%)");
    }

    /**
     * Step 7: Collect all results and export reports.
     */
    static void step7_collectResults(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 7: Collect Results & Export ===");

        // Export test run reports
        List<TestRun> testRuns = client.testRuns().list();
        for (TestRun run : testRuns) {
            try {
                byte[] report = client.testRuns().export(run.getId(), "json");
                System.out.println("Exported test run " + run.getId() + ": " + report.length + " bytes");
                // In CI: write to artifacts directory
                // Files.write(Path.of("reports/test-run-" + run.getId() + ".json"), report);
            } catch (Exception e) {
                System.err.println("Failed to export test run: " + e.getMessage());
            }
        }

        // Export fuzzing findings
        try {
            byte[] fuzzingExport = client.fuzzing().exportFindings(Map.of(
                    "format", "json"
            ));
            System.out.println("Exported fuzzing findings: " + fuzzingExport.length + " bytes");
            // Files.write(Path.of("reports/fuzzing-findings.json"), fuzzingExport);
        } catch (Exception e) {
            System.err.println("Failed to export fuzzing findings: " + e.getMessage());
        }

        // Get system stats for the report
        Map<String, Object> stats = client.stats().getStats();
        Map<String, Object> counts = client.stats().getCounts();
        System.out.println("Pipeline stats:");
        System.out.println("  Total requests processed: " + stats.get("totalRequests"));
        System.out.println("  Mocks in namespace: " + counts.get("mocks"));

        // Determine overall result
        result.passed = result.testsPassed &&
                result.contractsPassed &&
                result.fuzzingPassed &&
                result.perfPassed;

        result.steps.add("export: completed");
    }

    /**
     * Step 8: Clean up all pipeline resources.
     */
    static void step8_cleanup(MockartyClient client, PipelineResult result) {
        System.out.println("\n=== Step 8: Cleanup ===");

        try {
            // Delete all mocks in the pipeline namespace
            Page<Mock> mocks = client.mocks().list(PIPELINE_NAMESPACE, null, null, 0, 100);
            int deleted = 0;
            for (Mock mock : mocks.getItems()) {
                try {
                    client.mocks().delete(mock.getId());
                    deleted++;
                } catch (Exception e) {
                    // Continue cleanup on individual failures
                }
            }
            System.out.println("Deleted " + deleted + " mocks");

            // Delete test collections
            List<Map<String, Object>> collections = client.collections().list();
            for (Map<String, Object> col : collections) {
                try {
                    client.collections().delete(col.get("id").toString());
                } catch (Exception e) {
                    // Continue
                }
            }
            System.out.println("Cleaned up test collections");

            // Clean up fuzzing configs
            List<FuzzingConfig> fuzzConfigs = client.fuzzing().listConfigs();
            for (FuzzingConfig fc : fuzzConfigs) {
                try {
                    client.fuzzing().deleteConfig(fc.getId());
                } catch (Exception e) {
                    // Continue
                }
            }
            System.out.println("Cleaned up fuzzing configs");

            result.steps.add("cleanup: OK");

        } catch (Exception e) {
            System.err.println("Cleanup error: " + e.getMessage());
            result.steps.add("cleanup: ERROR - " + e.getMessage());
        }
    }

    // ---- Pipeline Result ----

    /**
     * Print the pipeline report summary.
     */
    static void printReport(PipelineResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  CI/CD PIPELINE REPORT");
        System.out.println("=".repeat(60));
        System.out.println("  Build ID:         " + result.buildId);
        System.out.println("  Overall Result:   " + (result.passed ? "PASSED" : "FAILED"));
        System.out.println("-".repeat(60));
        System.out.println("  Mocks generated:  " + result.mocksGenerated);
        System.out.println("  Tests:            " + result.testsTotal + " total, " +
                result.testsFailed + " failed " + (result.testsPassed ? "[PASS]" : "[FAIL]"));
        System.out.println("  Contracts:        " + (result.contractsPassed ? "PASSED" : "FAILED"));
        System.out.println("  Can deploy:       " + (result.canDeploy ? "YES" : "NO"));
        System.out.println("  Fuzzing:          " + result.fuzzingFindings + " findings, " +
                result.fuzzingCritical + " critical " + (result.fuzzingPassed ? "[PASS]" : "[FAIL]"));
        System.out.println("  Performance:      " + (result.perfPassed ? "PASSED" : "FAILED"));
        System.out.println("-".repeat(60));
        System.out.println("  Steps:");
        for (String step : result.steps) {
            System.out.println("    - " + step);
        }
        System.out.println("=".repeat(60));
    }

    /**
     * Pipeline result data holder.
     */
    static class PipelineResult {
        String buildId;
        boolean passed = true;
        int mocksGenerated = 0;
        boolean testsPassed = false;
        int testsTotal = 0;
        int testsFailed = 0;
        boolean contractsPassed = false;
        boolean canDeploy = false;
        boolean fuzzingPassed = false;
        int fuzzingFindings = 0;
        int fuzzingCritical = 0;
        boolean perfPassed = false;
        List<String> steps = new ArrayList<>();
        List<String> errors = new ArrayList<>();
    }
}
