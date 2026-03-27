// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.exception.MockartyApiException;
import ru.mockarty.exception.MockartyConnectionException;
import ru.mockarty.exception.MockartyNotFoundException;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Extract;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;
import ru.mockarty.model.SaveMockResponse;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Advanced patterns and best practices for using the Mockarty Java SDK.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Error handling and retries</li>
 *   <li>Bulk mock operations</li>
 *   <li>Cross-namespace operations</li>
 *   <li>Custom HTTP client configuration</li>
 *   <li>Template-based responses</li>
 *   <li>Complex request chains</li>
 *   <li>Mock lifecycle management</li>
 *   <li>Mock versioning (list, get, restore versions)</li>
 *   <li>Batch tag operations</li>
 * </ul>
 */
public class AdvancedExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            errorHandlingPatterns(client);
            bulkMockCreation(client);
            crossNamespaceOperations(client);
            templateResponses(client);
            requestChainWorkflow(client);
            mockLifecycleManagement(client);
            mockVersioning(client);
            batchTagOperations(client);
        }

        customHttpClientExample();
    }

    /**
     * Proper error handling with the SDK exception hierarchy.
     */
    static void errorHandlingPatterns(MockartyClient client) {
        // The SDK throws specific exception types for different error scenarios:
        //   MockartyConnectionException - server unreachable
        //   MockartyUnauthorizedException - 401, bad/missing API key
        //   MockartyForbiddenException - 403, insufficient permissions or license
        //   MockartyNotFoundException - 404, mock/resource not found
        //   MockartyApiException - all other HTTP errors (contains status code)

        // Pattern 1: Catch specific exceptions
        try {
            Mock mock = client.mocks().get("nonexistent-mock");
            System.out.println("Found: " + mock.getId());
        } catch (MockartyNotFoundException e) {
            System.out.println("Mock not found (expected): " + e.getMessage());
        } catch (MockartyApiException e) {
            System.out.println("API error " + e.getStatusCode() + ": " + e.getMessage());
        }

        // Pattern 2: Safe get-or-create
        try {
            Mock existing = client.mocks().get("my-mock-id");
            System.out.println("Mock already exists: " + existing.getId());
        } catch (MockartyNotFoundException e) {
            Mock newMock = MockBuilder.http("/api/test", "GET")
                    .id("my-mock-id")
                    .respond(200, Map.of("status", "ok"))
                    .build();
            client.mocks().create(newMock);
            System.out.println("Mock created (was not found)");
        }

        // Pattern 3: Connection resilience
        try {
            boolean healthy = client.health().ready();
            if (!healthy) {
                System.out.println("Server not ready, skipping operations");
                return;
            }
        } catch (MockartyConnectionException e) {
            System.err.println("Cannot connect to Mockarty: " + e.getMessage());
            return;
        }
    }

    /**
     * Bulk creation of many mocks efficiently.
     */
    static void bulkMockCreation(MockartyClient client) {
        List<String> createdIds = new ArrayList<>();
        int total = 10;
        int failed = 0;

        for (int i = 0; i < total; i++) {
            try {
                Mock mock = MockBuilder.http("/api/bulk/item-" + i, "GET")
                        .id("bulk-item-" + i)
                        .tags("bulk", "auto-generated")
                        .ttl(3600)  // Auto-cleanup after 1 hour
                        .respond(200, Map.of(
                                "itemId", i,
                                "name", "$.fake.Word",
                                "price", "$.fake.FloatRange(1.00,100.00)"
                        ))
                        .build();

                SaveMockResponse response = client.mocks().create(mock);
                createdIds.add(response.getMock().getId());
            } catch (MockartyApiException e) {
                failed++;
                System.err.println("Failed to create mock #" + i + ": " + e.getMessage());
            }
        }

        System.out.println("Bulk creation: " + createdIds.size() + "/" + total + " succeeded, " + failed + " failed");

        // Clean up bulk-created mocks
        for (String id : createdIds) {
            try {
                client.mocks().delete(id);
            } catch (MockartyApiException e) {
                // Log but continue cleanup
            }
        }
        System.out.println("Cleaned up " + createdIds.size() + " bulk mocks");
    }

    /**
     * Cross-namespace operations: copy mocks between namespaces.
     */
    static void crossNamespaceOperations(MockartyClient client) {
        // Create a mock in sandbox namespace
        Mock mock = MockBuilder.http("/api/shared/resource", "GET")
                .id("cross-ns-resource")
                .namespace("sandbox")
                .respond(200, Map.of("env", "sandbox", "data", "test"))
                .build();
        client.mocks().create(mock);

        // Copy mocks to another namespace
        client.mocks().copyToNamespace(
                List.of("cross-ns-resource"),
                "staging"
        );
        System.out.println("Copied mocks from sandbox to staging");

        // List mocks in the target namespace
        Page<Mock> stagingMocks = client.mocks().list("staging", null, null, 0, 10);
        System.out.println("Mocks in staging: " + stagingMocks.getTotal());
    }

    /**
     * Use custom HttpClient for fine-grained control (connection pool, proxy, SSL).
     */
    static void customHttpClientExample() {
        // Build a custom HttpClient with specific settings
        HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_2)
                .build();

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .httpClient(customClient)  // Inject custom HttpClient
                .timeout(Duration.ofSeconds(10))
                .build()) {

            boolean ready = client.health().ready();
            System.out.println("Custom client - server ready: " + ready);
        }
    }

    /**
     * Mocks using template paths for large response payloads.
     */
    static void templateResponses(MockartyClient client) {
        // Response from a pre-uploaded template file
        Mock fromTemplate = MockBuilder.http("/api/catalog/full", "GET")
                .id("advanced-template-response")
                .respondFromTemplate(200, "/templates/catalog-response.json")
                .build();

        client.mocks().create(fromTemplate);
        System.out.println("Created mock with template-based response");

        // Response referencing request data + Faker
        Mock dynamicResponse = MockBuilder.http("/api/echo", "POST")
                .id("advanced-echo-request")
                .respond(200, Map.of(
                        "echo", Map.of(
                                "body", "$.req",
                                "firstHeader", "$.reqHeader.Content-Type[0]",
                                "method", "POST",
                                "timestamp", "$.fake.DateISO"
                        ),
                        "serverInfo", Map.of(
                                "version", "$.gS.app.version",
                                "requestId", "$.fake.UUID"
                        )
                ))
                .build();

        client.mocks().create(dynamicResponse);
        System.out.println("Created dynamic echo mock");
    }

    /**
     * Complex request chain simulating a full e-commerce checkout flow.
     * Each step extracts data into the chain store for subsequent steps.
     */
    static void requestChainWorkflow(MockartyClient client) {
        String chainId = "checkout-flow";

        // Step 1: Add item to cart
        Mock addToCart = MockBuilder.http("/api/cart/items", "POST")
                .id("chain-add-to-cart")
                .chainId(chainId)
                .condition("productId", AssertAction.NOT_EMPTY, null)
                .extract(new Extract()
                        .cStore(Map.of(
                                "cartId", "$.fake.UUID",
                                "productId", "$.req.productId",
                                "quantity", "$.req.quantity"
                        ))
                )
                .respond(201, Map.of(
                        "cartId", "$.cS.cartId",
                        "items", List.of(Map.of(
                                "productId", "$.cS.productId",
                                "quantity", "$.cS.quantity",
                                "price", "$.fake.FloatRange(10.00,200.00)"
                        ))
                ))
                .build();

        // Step 2: Apply coupon
        Mock applyCoupon = MockBuilder.http("/api/cart/:cartId/coupon", "POST")
                .id("chain-apply-coupon")
                .chainId(chainId)
                .condition("code", AssertAction.NOT_EMPTY, null)
                .extract(new Extract()
                        .cStore(Map.of(
                                "couponCode", "$.req.code",
                                "discount", 15
                        ))
                )
                .respond(200, Map.of(
                        "cartId", "$.cS.cartId",
                        "couponCode", "$.cS.couponCode",
                        "discountPercent", "$.cS.discount",
                        "message", "Coupon applied"
                ))
                .build();

        // Step 3: Checkout
        Mock checkout = MockBuilder.http("/api/cart/:cartId/checkout", "POST")
                .id("chain-checkout")
                .chainId(chainId)
                .extract(new Extract()
                        .cStore(Map.of(
                                "orderId", "$.fake.UUID",
                                "status", "confirmed"
                        ))
                        .gStore(Map.of(
                                "lastOrderId", "$.fake.UUID",
                                "totalOrders", "$.increment($.gS.totalOrders)"
                        ))
                )
                .respond(200, Map.of(
                        "orderId", "$.cS.orderId",
                        "cartId", "$.cS.cartId",
                        "status", "confirmed",
                        "productId", "$.cS.productId",
                        "discount", "$.cS.discount",
                        "estimatedDelivery", "$.fake.DateISO"
                ))
                .build();

        // Step 4: Get order status (references chain data)
        Mock orderStatus = MockBuilder.http("/api/orders/:orderId/status", "GET")
                .id("chain-order-status")
                .chainId(chainId)
                .respond(200, Map.of(
                        "orderId", "$.cS.orderId",
                        "status", "$.cS.status",
                        "productId", "$.cS.productId",
                        "trackingUrl", "https://tracking.example.com/$.cS.orderId"
                ))
                .build();

        client.mocks().create(addToCart);
        client.mocks().create(applyCoupon);
        client.mocks().create(checkout);
        client.mocks().create(orderStatus);
        System.out.println("Created checkout workflow chain (4 steps)");
    }

    /**
     * Mock lifecycle management: create with TTL, check expiry,
     * and manage active mocks programmatically.
     */
    static void mockLifecycleManagement(MockartyClient client) {
        // Create ephemeral mocks for a test session
        List<String> testMockIds = new ArrayList<>();

        // Short-lived mocks with TTL and use limits
        for (int i = 0; i < 5; i++) {
            Mock mock = MockBuilder.http("/api/test-session/endpoint-" + i, "GET")
                    .id("lifecycle-test-" + i)
                    .ttl(300)        // 5-minute TTL
                    .useLimiter(10)  // Max 10 uses
                    .tags("test-session", "ephemeral")
                    .respond(200, Map.of("endpoint", i, "data", "$.fake.Sentence"))
                    .build();

            client.mocks().create(mock);
            testMockIds.add("lifecycle-test-" + i);
        }
        System.out.println("Created " + testMockIds.size() + " ephemeral test mocks");

        // Check mock status (use counter, expiry)
        for (String id : testMockIds) {
            Mock mock = client.mocks().get(id);
            System.out.println("  " + id +
                    " uses=" + mock.getUseCounter() + "/" + mock.getUseLimiter() +
                    " expires=" + mock.getExpireAt());
        }

        // List only test-session mocks using tag filter
        Page<Mock> testMocks = client.mocks().list("sandbox", List.of("test-session"), null, 0, 50);
        System.out.println("Active test-session mocks: " + testMocks.getTotal());

        // Clean up after test session
        for (String id : testMockIds) {
            client.mocks().delete(id);
        }
        System.out.println("Cleaned up test session mocks");
    }

    /**
     * Mock versioning: list, inspect, and restore previous versions of mocks.
     * Every time a mock is updated, Mockarty saves the previous version.
     */
    static void mockVersioning(MockartyClient client) {
        System.out.println("\n=== Mock Versioning ===");

        // Create a mock (version 1)
        Mock v1 = MockBuilder.http("/api/versioned/users", "GET")
                .id("versioned-mock")
                .tags("v1")
                .respond(200, Map.of(
                        "version", 1,
                        "users", List.of(
                                Map.of("id", "1", "name", "Alice")
                        )
                ))
                .build();
        client.mocks().create(v1);
        System.out.println("Created mock version 1");

        // Update the mock (version 2) - add more fields
        Mock v2 = MockBuilder.http("/api/versioned/users", "GET")
                .id("versioned-mock")
                .tags("v2")
                .respond(200, Map.of(
                        "version", 2,
                        "users", List.of(
                                Map.of("id", "1", "name", "Alice", "email", "alice@example.com"),
                                Map.of("id", "2", "name", "Bob", "email", "bob@example.com")
                        ),
                        "total", 2
                ))
                .build();
        client.mocks().create(v2);
        System.out.println("Updated mock to version 2");

        // Update again (version 3) - change structure
        Mock v3 = MockBuilder.http("/api/versioned/users", "GET")
                .id("versioned-mock")
                .tags("v3", "latest")
                .respond(200, Map.of(
                        "version", 3,
                        "data", Map.of(
                                "users", List.of(
                                        Map.of("id", "$.fake.UUID", "name", "$.fake.FirstName",
                                                "email", "$.fake.Email")
                                ),
                                "pagination", Map.of("page", 1, "total", 100)
                        )
                ))
                .build();
        client.mocks().create(v3);
        System.out.println("Updated mock to version 3");

        // List all versions
        List<Mock> versions = client.mocks().listVersions("versioned-mock");
        System.out.println("Mock versions: " + versions.size());
        for (Mock version : versions) {
            System.out.println("  Version: " + version.getVersion() +
                    " tags=" + version.getTags() +
                    " updatedAt=" + version.getUpdatedAt());
        }

        // Get a specific version
        if (versions.size() >= 2) {
            Mock oldVersion = client.mocks().getVersion("versioned-mock",
                    versions.get(1).getVersion());
            System.out.println("Retrieved old version: " + oldVersion.getVersion());
            System.out.println("  Tags: " + oldVersion.getTags());
        }

        // Restore a previous version
        if (versions.size() >= 2) {
            client.mocks().restoreVersion("versioned-mock",
                    versions.get(1).getVersion());
            System.out.println("Restored mock to version: " + versions.get(1).getVersion());

            // Verify the restore
            Mock restored = client.mocks().get("versioned-mock");
            System.out.println("Current mock tags after restore: " + restored.getTags());
        }

        // Clean up
        client.mocks().delete("versioned-mock");
        System.out.println("Deleted versioned mock");
    }

    /**
     * Batch tag operations for organizing multiple mocks at once.
     */
    static void batchTagOperations(MockartyClient client) {
        System.out.println("\n=== Batch Tag Operations ===");

        // Create several mocks
        List<String> mockIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Mock mock = MockBuilder.http("/api/batch-tag/endpoint-" + i, "GET")
                    .id("batch-tag-" + i)
                    .tags("initial")
                    .respond(200, Map.of("index", i))
                    .build();
            client.mocks().create(mock);
            mockIds.add("batch-tag-" + i);
        }
        System.out.println("Created " + mockIds.size() + " mocks with 'initial' tag");

        // Batch update tags for all mocks
        client.mocks().batchUpdateTags(
                mockIds,
                List.of("production", "v2", "validated", "critical-path")
        );
        System.out.println("Applied tags [production, v2, validated, critical-path] to " +
                mockIds.size() + " mocks");

        // Verify tags were applied
        for (String id : mockIds) {
            Mock mock = client.mocks().get(id);
            System.out.println("  " + id + " tags: " + mock.getTags());
        }

        // Filter by the new tags
        Page<Mock> taggedMocks = client.mocks().list("sandbox", List.of("production", "v2"), null, 0, 50);
        System.out.println("Mocks with 'production' + 'v2' tags: " + taggedMocks.getTotal());

        // Partial update: use patch to update just tags on a single mock
        client.mocks().patchMock("batch-tag-0", Map.of(
                "tags", List.of("production", "v2", "validated", "critical-path", "hot-fix")
        ));
        System.out.println("Patched mock 'batch-tag-0' with additional 'hot-fix' tag");

        // Move mocks to a folder (if folder exists)
        // client.mocks().moveToFolder(mockIds, "folder-id-here");

        // Clean up
        for (String id : mockIds) {
            client.mocks().delete(id);
        }
        System.out.println("Cleaned up batch-tag mocks");
    }
}
