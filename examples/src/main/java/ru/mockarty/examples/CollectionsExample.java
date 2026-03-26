// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.TestRun;

import java.util.List;
import java.util.Map;

/**
 * Collections and Test Runs examples showing how to organize API tests,
 * run collections, and export test results.
 */
public class CollectionsExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createAndRunCollection(client);
            manageTestRuns(client);
        }
    }

    /**
     * Create a test collection with multiple requests and run it.
     */
    static void createAndRunCollection(MockartyClient client) {
        // Define a test collection with API requests
        Map<String, Object> collection = Map.of(
                "name", "User Service Smoke Test",
                "namespace", "sandbox",
                "description", "Basic smoke test for user service endpoints",
                "requests", List.of(
                        Map.of(
                                "name", "Health Check",
                                "method", "GET",
                                "url", "http://localhost:5770/health",
                                "expectedStatus", 200
                        ),
                        Map.of(
                                "name", "List Users",
                                "method", "GET",
                                "url", "http://localhost:5770/api/users",
                                "headers", Map.of(
                                        "Accept", "application/json"
                                ),
                                "expectedStatus", 200
                        ),
                        Map.of(
                                "name", "Create User",
                                "method", "POST",
                                "url", "http://localhost:5770/api/users",
                                "headers", Map.of(
                                        "Content-Type", "application/json"
                                ),
                                "body", Map.of(
                                        "name", "Test User",
                                        "email", "test@example.com"
                                ),
                                "expectedStatus", 201
                        ),
                        Map.of(
                                "name", "Get User by ID",
                                "method", "GET",
                                "url", "http://localhost:5770/api/users/test-user-001",
                                "expectedStatus", 200
                        )
                ),
                "settings", Map.of(
                        "stopOnFailure", false,
                        "timeout", 10000,
                        "retryFailedRequests", false
                )
        );

        // Create the collection
        Map<String, Object> created = client.collections().create(collection);
        String collectionId = (String) created.get("id");
        System.out.println("Created collection: " + collectionId);

        // List all collections
        List<Map<String, Object>> allCollections = client.collections().list();
        System.out.println("Total collections: " + allCollections.size());

        // Get collection details
        Map<String, Object> details = client.collections().get(collectionId);
        System.out.println("Collection name: " + details.get("name"));

        // Run the collection
        Map<String, Object> runResult = client.collections().run(collectionId);
        System.out.println("Run result: " + runResult);
        System.out.println("  Total requests: " + runResult.get("totalRequests"));
        System.out.println("  Passed: " + runResult.get("passed"));
        System.out.println("  Failed: " + runResult.get("failed"));

        // Clean up
        client.collections().delete(collectionId);
        System.out.println("Collection deleted");
    }

    /**
     * Manage test runs: list, inspect, export, and clean up.
     */
    static void manageTestRuns(MockartyClient client) {
        // List all test runs
        List<TestRun> testRuns = client.testRuns().list();
        System.out.println("Total test runs: " + testRuns.size());

        if (!testRuns.isEmpty()) {
            // Get details of the most recent test run
            TestRun latest = testRuns.get(0);
            String runId = latest.getId();

            TestRun details = client.testRuns().get(runId);
            System.out.println("Test run: " + details.getId());
            System.out.println("  Status: " + details.getStatus());

            // Export test run as JSON
            byte[] jsonReport = client.testRuns().export(runId, "json");
            System.out.println("Exported JSON report: " + jsonReport.length + " bytes");

            // Cancel a running test (if it's still running)
            // client.testRuns().cancel(runId);

            // Delete old test runs
            // client.testRuns().delete(runId);
        }
    }
}
