// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.HealthResponse;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;
import ru.mockarty.model.SaveMockResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Quick-start example showing basic Mockarty Java SDK usage.
 *
 * <p>Covers client initialization, creating a mock, verifying it,
 * calling the mocked endpoint, and cleaning up.</p>
 */
public class BasicExample {

    public static void main(String[] args) {
        // --- 1. Create a client ---

        // Option A: Builder with explicit configuration
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .timeout(Duration.ofSeconds(15))
                .build()) {

            runExamples(client);
        }

        // Option B: Shorthand factory (reads MOCKARTY_BASE_URL, MOCKARTY_API_KEY env vars)
        try (MockartyClient client = MockartyClient.create()) {
            System.out.println("Client from env vars: " + client.getConfig());
        }

        // Option C: URL-only factory
        try (MockartyClient client = MockartyClient.create("http://localhost:5770")) {
            System.out.println("Client from URL: " + client.getConfig());
        }

        // Option D: URL + API key factory
        try (MockartyClient client = MockartyClient.create("http://localhost:5770", "your-api-key")) {
            System.out.println("Client from URL + key: " + client.getConfig());
        }
    }

    private static void runExamples(MockartyClient client) {
        // --- 2. Health check ---
        boolean serverReady = client.health().ready();
        String version = client.health().version();
        System.out.println("Server ready: " + serverReady + ", version: " + version);

        // --- 3. Create a simple GET mock ---
        Mock getUserMock = MockBuilder.http("/api/users/:id", "GET")
                .id("example-user-get")
                .namespace("sandbox")
                .tags("users", "example")
                .respond(200, Map.of(
                        "id", "$.pathParam.id",
                        "name", "$.fake.FirstName",
                        "email", "$.fake.Email",
                        "createdAt", "$.fake.DateISO"
                ))
                .build();

        SaveMockResponse saved = client.mocks().create(getUserMock);
        System.out.println("Created mock: " + saved.getMock().getId());
        System.out.println("Was overwrite: " + saved.isOverwrite());

        // --- 4. Retrieve the mock back ---
        Mock fetched = client.mocks().get("example-user-get");
        System.out.println("Fetched mock protocol: " + fetched.protocol());

        // --- 5. List mocks in the namespace ---
        Page<Mock> page = client.mocks().list("sandbox", null, null, 0, 10);
        System.out.println("Total mocks in sandbox: " + page.getTotal());

        // --- 6. Search mocks by tag ---
        Page<Mock> tagged = client.mocks().list("sandbox", List.of("users"), null, 0, 50);
        System.out.println("Mocks tagged 'users': " + tagged.getTotal());

        // --- 7. View request logs ---
        List<Map<String, Object>> logs = client.mocks().logs("example-user-get");
        System.out.println("Request log count: " + logs.size());

        // --- 8. Delete the mock ---
        client.mocks().delete("example-user-get");
        System.out.println("Mock deleted");

        // --- 9. Restore a soft-deleted mock ---
        Mock restored = client.mocks().restore("example-user-get");
        System.out.println("Mock restored: " + restored.getId());

        // --- 10. Permanently purge ---
        client.mocks().purge("example-user-get");
        System.out.println("Mock purged permanently");
    }
}
