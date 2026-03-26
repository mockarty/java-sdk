// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.junit5.MockartyExtension;
import ru.mockarty.junit5.MockartyServer;
import ru.mockarty.junit5.MockartyTest;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 integration examples demonstrating the @MockartyTest annotation,
 * parameter injection, automatic mock cleanup, and real test patterns.
 *
 * <h2>Test Setup</h2>
 * <p>Set environment variables before running:</p>
 * <pre>
 *   MOCKARTY_BASE_URL=http://localhost:5770
 *   MOCKARTY_API_KEY=your-api-key
 * </pre>
 *
 * <p>Or configure via the @MockartyTest annotation parameters.</p>
 */
@MockartyTest(namespace = "test-sandbox", cleanupAfterEach = true)
@DisplayName("Mockarty JUnit 5 Integration Examples")
class JUnit5Example {

    // ---- Basic Mock Creation ----

    @Test
    @DisplayName("Create a simple GET mock and verify it exists")
    void testSimpleMockCreation(MockartyClient client, MockartyServer server) {
        // Create a mock using the server (tracked for auto-cleanup)
        Mock mock = MockBuilder.http("/api/test/hello", "GET")
                .id("junit5-hello")
                .respond(200, Map.of("message", "Hello, World!"))
                .build();

        Mock created = server.createMock(mock);

        // Verify the mock was created
        assertNotNull(created.getId());
        assertEquals("junit5-hello", created.getId());

        // Verify we can retrieve it
        Mock fetched = client.mocks().get("junit5-hello");
        assertNotNull(fetched);
        assertEquals(200, fetched.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Create mock with conditions and verify matching")
    void testMockWithConditions(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/users", "POST")
                .id("junit5-create-user")
                .condition("email", AssertAction.NOT_EMPTY, null)
                .condition("name", AssertAction.NOT_EMPTY, null)
                .respond(201, Map.of(
                        "id", "$.fake.UUID",
                        "name", "$.req.name",
                        "email", "$.req.email"
                ))
                .build();

        server.createMock(mock);

        // Verify the mock is retrievable
        Mock fetched = client.mocks().get("junit5-create-user");
        assertNotNull(fetched.getHttp());
        assertEquals("/api/test/users", fetched.getHttp().getRoute());
    }

    // ---- Auto-Cleanup Verification ----

    @Test
    @DisplayName("Verify auto-cleanup: mocks from previous test are gone")
    void testAutoCleanup(MockartyClient client, MockartyServer server) {
        // The mock "junit5-hello" from the previous test should have been
        // cleaned up automatically (cleanupAfterEach = true)

        // Create a new mock for this test
        Mock mock = MockBuilder.http("/api/test/cleanup-check", "GET")
                .id("junit5-cleanup-check")
                .respond(200, Map.of("cleanupWorks", true))
                .build();

        server.createMock(mock);
        assertEquals(1, server.createdMockCount());
    }

    // ---- Server Tracking ----

    @Test
    @DisplayName("MockartyServer tracks all created mocks")
    void testServerTracking(MockartyClient client, MockartyServer server) {
        // Initially no mocks tracked
        assertEquals(0, server.createdMockCount());
        assertTrue(server.getCreatedMockIds().isEmpty());

        // Create several mocks
        for (int i = 0; i < 3; i++) {
            Mock mock = MockBuilder.http("/api/test/tracked-" + i, "GET")
                    .id("junit5-tracked-" + i)
                    .respond(200, Map.of("index", i))
                    .build();
            server.createMock(mock);
        }

        // All mocks should be tracked
        assertEquals(3, server.createdMockCount());
        assertEquals(3, server.getCreatedMockIds().size());
        assertTrue(server.getCreatedMockIds().contains("junit5-tracked-0"));
        assertTrue(server.getCreatedMockIds().contains("junit5-tracked-1"));
        assertTrue(server.getCreatedMockIds().contains("junit5-tracked-2"));
    }

    // ---- Response Patterns ----

    @Test
    @DisplayName("OneOf ordered responses return different results per call")
    void testOneOfOrdered(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/oneof-ordered", "GET")
                .id("junit5-oneof-ordered")
                .oneOfOrdered(
                        new ContentResponse().statusCode(200).payload(Map.of("step", 1)),
                        new ContentResponse().statusCode(200).payload(Map.of("step", 2)),
                        new ContentResponse().statusCode(200).payload(Map.of("step", 3))
                )
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-oneof-ordered");
        assertNotNull(fetched.getOneOf());
        assertEquals("order", fetched.getOneOf().getOrder());
        assertEquals(3, fetched.getOneOf().getResponses().size());
    }

    @Test
    @DisplayName("Mock with delay is properly configured")
    void testMockWithDelay(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/delayed", "GET")
                .id("junit5-delayed")
                .respondWithDelay(200, Map.of("data", "slow"), 1500)
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-delayed");
        assertEquals(1500, fetched.getResponse().getDelay());
    }

    // ---- Multi-Protocol Mocks ----

    @Test
    @DisplayName("Create gRPC mock via JUnit extension")
    void testGrpcMock(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.grpc("test.TestService", "SayHello")
                .id("junit5-grpc-hello")
                .respond(200, Map.of("message", "Hello from gRPC mock"))
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-grpc-hello");
        assertNotNull(fetched.getGrpc());
        assertEquals("test.TestService", fetched.getGrpc().getService());
        assertEquals("SayHello", fetched.getGrpc().getMethod());
    }

    @Test
    @DisplayName("Create GraphQL mock via JUnit extension")
    void testGraphqlMock(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.graphql("query", "testQuery")
                .id("junit5-graphql-query")
                .respond(200, Map.of(
                        "data", Map.of(
                                "testQuery", Map.of("result", "ok")
                        )
                ))
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-graphql-query");
        assertNotNull(fetched.getGraphql());
        assertEquals("query", fetched.getGraphql().getOperation());
    }

    // ---- Error Scenarios ----

    @Test
    @DisplayName("Mock with error response returns correct status code")
    void testErrorResponse(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/forbidden", "GET")
                .id("junit5-forbidden")
                .respondWithError(403, "Access denied")
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-forbidden");
        assertEquals(403, fetched.getResponse().getStatusCode());
        assertEquals("Access denied", fetched.getResponse().getError());
    }

    // ---- Advanced Mock Features ----

    @Test
    @DisplayName("Mock with TTL and use limiter")
    void testMockWithLimits(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/limited", "GET")
                .id("junit5-limited")
                .ttl(600)         // 10-minute TTL
                .useLimiter(5)    // Max 5 uses
                .respond(200, Map.of("limited", true))
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-limited");
        assertEquals(600L, fetched.getTtl());
        assertEquals(5, fetched.getUseLimiter());
    }

    @Test
    @DisplayName("Mock with tags and priority")
    void testMockWithMetadata(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/tagged", "GET")
                .id("junit5-tagged")
                .tags("junit", "example", "v2")
                .priority(50)
                .respond(200, Map.of("tagged", true))
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-tagged");
        assertTrue(fetched.getTags().contains("junit"));
        assertTrue(fetched.getTags().contains("example"));
        assertEquals(50L, fetched.getPriority());
    }

    @Test
    @DisplayName("Mock with proxy configuration")
    void testMockWithProxy(MockartyClient client, MockartyServer server) {
        Mock mock = MockBuilder.http("/api/test/proxy-target", "GET")
                .id("junit5-proxy")
                .proxyTo("https://httpbin.org")
                .build();

        server.createMock(mock);

        Mock fetched = client.mocks().get("junit5-proxy");
        assertNotNull(fetched.getProxy());
        assertEquals("https://httpbin.org", fetched.getProxy().getTarget());
    }

    // ---- Nested Test Class ----

    /**
     * Nested test class demonstrating a separate @MockartyTest configuration.
     * Uses @ExtendWith directly instead of @MockartyTest.
     */
    @Nested
    @ExtendWith(MockartyExtension.class)
    @DisplayName("Tests with manual extension registration")
    class ManualExtensionTests {

        @Test
        @DisplayName("Extension works without @MockartyTest annotation")
        void testWithoutAnnotation(MockartyClient client, MockartyServer server) {
            // When using @ExtendWith directly, defaults are used:
            // - namespace: "sandbox"
            // - baseUrl: from env or http://localhost:5770
            // - cleanup: true (default)

            Mock mock = MockBuilder.http("/api/test/manual", "GET")
                    .id("junit5-manual-ext")
                    .respond(200, Map.of("mode", "manual"))
                    .build();

            server.createMock(mock);
            assertNotNull(client.mocks().get("junit5-manual-ext"));
        }
    }

    // ---- Integration Test Pattern ----

    @Test
    @DisplayName("End-to-end: create mock then call it with HttpClient")
    void testEndToEndWithRealHttpCall(MockartyClient client, MockartyServer server) throws Exception {
        // Create a mock
        Mock mock = MockBuilder.http("/api/test/e2e-endpoint", "GET")
                .id("junit5-e2e")
                .respond(200, Map.of("status", "ok", "source", "mock"))
                .build();

        server.createMock(mock);

        // Now call the mock endpoint directly using Java HttpClient
        String baseUrl = client.getConfig().getBaseUrl();
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/test/e2e-endpoint"))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("ok"));
        assertTrue(response.body().contains("mock"));
    }

    @Test
    @DisplayName("Test chain of related mocks")
    void testMockChain(MockartyClient client, MockartyServer server) {
        String chainId = "junit5-chain";

        Mock step1 = MockBuilder.http("/api/test/chain/step1", "POST")
                .id("junit5-chain-step1")
                .chainId(chainId)
                .respond(201, Map.of("step", 1, "next", "/api/test/chain/step2"))
                .build();

        Mock step2 = MockBuilder.http("/api/test/chain/step2", "POST")
                .id("junit5-chain-step2")
                .chainId(chainId)
                .respond(200, Map.of("step", 2, "status", "completed"))
                .build();

        server.createMock(step1);
        server.createMock(step2);

        // Verify chain
        List<Mock> chain = client.mocks().getChain(chainId);
        assertEquals(2, chain.size());

        // Both mocks should be tracked for cleanup
        assertEquals(2, server.createdMockCount());
    }
}
