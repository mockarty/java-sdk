// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.Map;

/**
 * System statistics and monitoring examples demonstrating
 * how to query Mockarty's stats, counts, status, and features.
 *
 * <p>Useful for:</p>
 * <ul>
 *   <li>Dashboard integrations and monitoring</li>
 *   <li>CI/CD pipeline health checks</li>
 *   <li>Feature availability detection</li>
 *   <li>Capacity planning and resource tracking</li>
 * </ul>
 */
public class StatsExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            systemStats(client);
            resourceCounts(client);
            systemStatus(client);
            featureDetection(client);
        }
    }

    /**
     * Get general system statistics (request counts, latency, etc.).
     */
    static void systemStats(MockartyClient client) {
        System.out.println("=== System Statistics ===");

        Map<String, Object> stats = client.stats().getStats();
        System.out.println("System stats:");
        System.out.println("  Total requests: " + stats.get("totalRequests"));
        System.out.println("  Matched requests: " + stats.get("matchedRequests"));
        System.out.println("  Unmatched requests: " + stats.get("unmatchedRequests"));
        System.out.println("  Avg response time: " + stats.get("avgResponseTimeMs") + "ms");
        System.out.println("  Uptime: " + stats.get("uptimeSeconds") + "s");

        // Calculate match rate
        Object totalObj = stats.get("totalRequests");
        Object matchedObj = stats.get("matchedRequests");
        if (totalObj instanceof Number && matchedObj instanceof Number) {
            long total = ((Number) totalObj).longValue();
            long matched = ((Number) matchedObj).longValue();
            if (total > 0) {
                double matchRate = (double) matched / total * 100;
                System.out.printf("  Match rate: %.1f%%%n", matchRate);
            }
        }
    }

    /**
     * Get resource counts (mocks, namespaces, stores, etc.).
     */
    static void resourceCounts(MockartyClient client) {
        System.out.println("\n=== Resource Counts ===");

        Map<String, Object> counts = client.stats().getCounts();
        System.out.println("Resource counts:");
        System.out.println("  Mocks: " + counts.get("mocks"));
        System.out.println("  Namespaces: " + counts.get("namespaces"));
        System.out.println("  Global store keys: " + counts.get("globalStoreKeys"));
        System.out.println("  Chain store keys: " + counts.get("chainStoreKeys"));
        System.out.println("  Active sessions: " + counts.get("activeSessions"));
        System.out.println("  Test collections: " + counts.get("collections"));
        System.out.println("  Contract configs: " + counts.get("contractConfigs"));
        System.out.println("  Fuzzing configs: " + counts.get("fuzzingConfigs"));
    }

    /**
     * Check the current system status for health monitoring.
     */
    static void systemStatus(MockartyClient client) {
        System.out.println("\n=== System Status ===");

        Map<String, Object> status = client.stats().getStatus();
        System.out.println("System status:");
        System.out.println("  Status: " + status.get("status"));
        System.out.println("  Version: " + status.get("version"));
        System.out.println("  Database: " + status.get("database"));
        System.out.println("  Cache: " + status.get("cache"));
        System.out.println("  License: " + status.get("license"));

        // Check if the system is healthy
        boolean isHealthy = "ok".equals(status.get("status"));
        System.out.println("  Healthy: " + isHealthy);

        // Also use the health API for a quick check
        boolean ready = client.health().ready();
        System.out.println("  Ready: " + ready);
    }

    /**
     * Detect available features for conditional logic in automation.
     */
    static void featureDetection(MockartyClient client) {
        System.out.println("\n=== Feature Detection ===");

        Map<String, Object> features = client.stats().getFeatures();
        System.out.println("Available features:");

        for (Map.Entry<String, Object> entry : features.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        // Conditional logic based on available features
        if (Boolean.TRUE.equals(features.get("fuzzing"))) {
            System.out.println("\nFuzzing is available - can run security tests");
        }

        if (Boolean.TRUE.equals(features.get("contractTesting"))) {
            System.out.println("Contract testing is available - can validate contracts");
        }

        if (Boolean.TRUE.equals(features.get("perfTesting"))) {
            System.out.println("Performance testing is available - can run load tests");
        }

        if (Boolean.TRUE.equals(features.get("aiAgent"))) {
            System.out.println("AI agent is available - can submit agent tasks");
        }

        if (Boolean.TRUE.equals(features.get("recorder"))) {
            System.out.println("Recorder is available - can capture live traffic");
        }
    }
}
