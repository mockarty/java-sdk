// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.Mock;
import ru.mockarty.model.UndefinedRequest;

import java.util.List;

/**
 * Undefined (unmatched) request management examples demonstrating
 * how to discover missing mocks and create them from captured traffic.
 *
 * <p>When Mockarty receives a request that doesn't match any mock,
 * it records it as an "undefined request". This API lets you:</p>
 * <ul>
 *   <li>List all unmatched requests to find coverage gaps</li>
 *   <li>Create mocks automatically from captured undefined requests</li>
 *   <li>Ignore known undefined requests (e.g., health checks)</li>
 *   <li>Clean up undefined request logs</li>
 * </ul>
 */
public class UndefinedRequestsExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            listUndefinedRequests(client);
            createMocksFromUndefined(client);
            ignoreAndCleanup(client);
        }
    }

    /**
     * List all undefined (unmatched) requests to identify missing mocks.
     */
    static void listUndefinedRequests(MockartyClient client) {
        System.out.println("=== Undefined Requests ===");

        List<UndefinedRequest> requests = client.undefined().list();
        System.out.println("Total undefined requests: " + requests.size());

        for (UndefinedRequest req : requests) {
            System.out.println("  " + req.getMethod() + " " + req.getPath());
            System.out.println("    Namespace: " + req.getNamespace());
            System.out.println("    Count: " + req.getCount());
            System.out.println("    First seen: " + req.getFirstSeen());
            System.out.println("    Last seen: " + req.getLastSeen());
        }

        // Group by path to identify most common missing mocks
        System.out.println("\nMost frequently missed endpoints:");
        requests.stream()
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(5)
                .forEach(req -> System.out.println("  " + req.getCount() + "x " +
                        req.getMethod() + " " + req.getPath()));
    }

    /**
     * Create mocks from undefined requests to fill coverage gaps.
     * Each undefined request is converted into a mock with a default response.
     */
    static void createMocksFromUndefined(MockartyClient client) {
        System.out.println("\n=== Create Mocks from Undefined ===");

        List<UndefinedRequest> requests = client.undefined().list();
        if (requests.isEmpty()) {
            System.out.println("No undefined requests to create mocks from");
            return;
        }

        int created = 0;
        for (UndefinedRequest req : requests) {
            // Skip health checks and metrics endpoints
            if (req.getPath().contains("/health") || req.getPath().contains("/metrics")) {
                System.out.println("Skipping: " + req.getPath());
                continue;
            }

            try {
                // Create a mock from the undefined request
                Mock mock = client.undefined().createMock(req.getId());
                System.out.println("Created mock from undefined request:");
                System.out.println("  Mock ID: " + mock.getId());
                System.out.println("  Route: " + mock.getHttp().getRoute());
                System.out.println("  Method: " + mock.getHttp().getHttpMethod());
                created++;
            } catch (Exception e) {
                System.err.println("Failed to create mock for " +
                        req.getMethod() + " " + req.getPath() + ": " + e.getMessage());
            }
        }

        System.out.println("Created " + created + " mocks from undefined requests");
    }

    /**
     * Ignore known undefined requests and clean up the log.
     */
    static void ignoreAndCleanup(MockartyClient client) {
        System.out.println("\n=== Ignore and Cleanup ===");

        List<UndefinedRequest> requests = client.undefined().list();
        if (requests.isEmpty()) {
            System.out.println("No undefined requests to manage");
            return;
        }

        // Ignore specific undefined requests (e.g., health checks, favicon)
        for (UndefinedRequest req : requests) {
            if (req.getPath().contains("/health") ||
                req.getPath().contains("/favicon") ||
                req.getPath().contains("/metrics")) {
                client.undefined().ignore(req.getId());
                System.out.println("Ignored: " + req.getMethod() + " " + req.getPath());
            }
        }

        // Delete specific undefined requests
        if (requests.size() >= 2) {
            client.undefined().delete(List.of(
                    requests.get(0).getId(),
                    requests.get(1).getId()
            ));
            System.out.println("Deleted 2 specific undefined requests");
        }

        // Clear all undefined requests (use with caution)
        // client.undefined().clearAll();
        // System.out.println("Cleared all undefined requests");

        // Verify remaining
        List<UndefinedRequest> remaining = client.undefined().list();
        System.out.println("Remaining undefined requests: " + remaining.size());
    }
}
