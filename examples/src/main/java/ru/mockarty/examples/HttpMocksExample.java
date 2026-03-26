// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.AssertAction;
import ru.mockarty.model.ContentResponse;
import ru.mockarty.model.Mock;

import java.util.List;
import java.util.Map;

/**
 * HTTP mock examples covering GET/POST/PUT with conditions,
 * Faker templates, OneOf responses, delay, TTL, and use limiters.
 */
public class HttpMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleGetMock(client);
            createGetWithQueryConditions(client);
            createPostWithBodyConditions(client);
            createPutWithHeaderConditions(client);
            createMockWithFakerTemplates(client);
            createMockWithOneOfOrdered(client);
            createMockWithOneOfRandom(client);
            createMockWithDelay(client);
            createMockWithTtl(client);
            createMockWithUseLimiter(client);
            createMockWithCustomHeaders(client);
            createMockWithErrorResponse(client);
            createMockWithPriority(client);
        }
    }

    /**
     * Simple GET endpoint returning a user object.
     */
    static void createSimpleGetMock(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/users/:id", "GET")
                .id("http-get-user")
                .respond(200, Map.of(
                        "id", "$.pathParam.id",
                        "name", "John Doe",
                        "email", "john@example.com"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple GET mock");
    }

    /**
     * GET with query parameter conditions.
     * Matches only when ?status=active&role=admin are present.
     */
    static void createGetWithQueryConditions(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/users", "GET")
                .id("http-get-users-filtered")
                .queryCondition("status", AssertAction.EQUALS, "active")
                .queryCondition("role", AssertAction.EQUALS, "admin")
                .respond(200, Map.of(
                        "users", List.of(
                                Map.of("id", 1, "name", "Admin User", "role", "admin"),
                                Map.of("id", 2, "name", "Super Admin", "role", "admin")
                        ),
                        "total", 2
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created GET with query conditions");
    }

    /**
     * POST with JSON body conditions.
     * Matches when the body contains specific field values.
     */
    static void createPostWithBodyConditions(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/users", "POST")
                .id("http-post-create-user")
                .condition("email", AssertAction.NOT_EMPTY, null)
                .condition("name", AssertAction.CONTAINS, "@")
                .condition("age", AssertAction.NOT_EMPTY, null)
                .headerCondition("Content-Type", AssertAction.CONTAINS, "application/json")
                .respond(201, Map.of(
                        "id", "$.fake.UUID",
                        "name", "$.req.name",
                        "email", "$.req.email",
                        "createdAt", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created POST with body conditions");
    }

    /**
     * PUT with header-based conditions.
     * Matches only with a valid Authorization header.
     */
    static void createPutWithHeaderConditions(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/users/:id", "PUT")
                .id("http-put-update-user")
                .headerCondition("Authorization", AssertAction.MATCHES, "^Bearer .+$")
                .headerCondition("X-Request-ID", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "id", "$.pathParam.id",
                        "name", "$.req.name",
                        "updatedAt", "$.fake.DateISO",
                        "updatedBy", "$.reqHeader.X-Request-ID[0]"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created PUT with header conditions");
    }

    /**
     * Mock demonstrating various Faker template functions.
     */
    static void createMockWithFakerTemplates(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/fake-data", "GET")
                .id("http-faker-demo")
                .respond(200, Map.of(
                        "uuid", "$.fake.UUID",
                        "firstName", "$.fake.FirstName",
                        "lastName", "$.fake.LastName",
                        "email", "$.fake.Email",
                        "phone", "$.fake.PhoneNumber",
                        "address", Map.of(
                                "street", "$.fake.StreetAddress",
                                "city", "$.fake.City",
                                "country", "$.fake.Country",
                                "zip", "$.fake.ZipCode"
                        ),
                        "company", "$.fake.Company",
                        "creditCard", "$.fake.CreditCardNumber",
                        "ipv4", "$.fake.IPv4",
                        "userAgent", "$.fake.UserAgent",
                        "paragraph", "$.fake.Paragraph",
                        "randomInt", "$.fake.IntRange(1,1000)",
                        "price", "$.fake.FloatRange(9.99,999.99)",
                        "boolean", "$.fake.Bool",
                        "date", "$.fake.DateISO",
                        "color", "$.fake.HexColor",
                        "word", "$.fake.Word"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with Faker templates");
    }

    /**
     * OneOf with ordered responses -- returns different responses in sequence.
     * First call -> 200, second call -> 201, third call -> 200 again (cycles).
     */
    static void createMockWithOneOfOrdered(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/orders/:id/status", "GET")
                .id("http-oneof-ordered")
                .oneOfOrdered(
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "pending", "step", 1)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "processing", "step", 2)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "shipped", "step", 3)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "delivered", "step", 4))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created OneOf ordered mock (order lifecycle)");
    }

    /**
     * OneOf with random responses -- simulates a flaky service.
     */
    static void createMockWithOneOfRandom(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/external/payment", "POST")
                .id("http-oneof-random-flaky")
                .oneOfRandom(
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "success", "transactionId", "$.fake.UUID")),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("status", "success", "transactionId", "$.fake.UUID")),
                        new ContentResponse()
                                .statusCode(500)
                                .payload(Map.of("error", "Internal server error")),
                        new ContentResponse()
                                .statusCode(503)
                                .payload(Map.of("error", "Service temporarily unavailable"))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created OneOf random mock (flaky payment service)");
    }

    /**
     * Mock with a fixed delay to simulate slow responses.
     */
    static void createMockWithDelay(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/reports/generate", "POST")
                .id("http-slow-report")
                .respondWithDelay(200,
                        Map.of(
                                "reportId", "$.fake.UUID",
                                "status", "completed",
                                "generatedAt", "$.fake.DateISO"
                        ),
                        2000  // 2-second delay
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with 2s delay");
    }

    /**
     * Mock with TTL -- auto-expires after 1 hour.
     */
    static void createMockWithTtl(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/promo/flash-sale", "GET")
                .id("http-ttl-promo")
                .ttl(3600)  // 1 hour TTL
                .respond(200, Map.of(
                        "sale", "Flash Sale!",
                        "discount", "50%",
                        "expiresIn", "1 hour"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with 1-hour TTL");
    }

    /**
     * Mock with use limiter -- responds only N times, then stops matching.
     */
    static void createMockWithUseLimiter(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/onboarding/welcome", "GET")
                .id("http-use-limiter")
                .useLimiter(3)  // Only match 3 times
                .respond(200, Map.of(
                        "message", "Welcome! This is a one-time greeting.",
                        "remainingUses", "limited"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with 3-use limiter");
    }

    /**
     * Mock with custom response headers.
     */
    static void createMockWithCustomHeaders(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/download/file", "GET")
                .id("http-custom-headers")
                .respond(200,
                        Map.of("fileUrl", "https://cdn.example.com/file.pdf"),
                        Map.of(
                                "Content-Type", List.of("application/json"),
                                "Cache-Control", List.of("no-cache, no-store"),
                                "X-RateLimit-Remaining", List.of("99"),
                                "X-Request-ID", List.of("$.fake.UUID")
                        )
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with custom headers");
    }

    /**
     * Mock returning an error response.
     */
    static void createMockWithErrorResponse(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/protected/resource", "GET")
                .id("http-error-forbidden")
                .respondWithError(403, "Access denied: insufficient permissions")
                .build();

        client.mocks().create(mock);
        System.out.println("Created error response mock");
    }

    /**
     * Mock with priority -- higher priority mocks are matched first.
     */
    static void createMockWithPriority(MockartyClient client) {
        // Low-priority catch-all
        Mock catchAll = MockBuilder.http("/api/items/:id", "GET")
                .id("http-items-catch-all")
                .priority(1)
                .respond(200, Map.of("id", "$.pathParam.id", "type", "generic"))
                .build();

        // High-priority specific condition
        Mock specific = MockBuilder.http("/api/items/:id", "GET")
                .id("http-items-premium")
                .priority(100)
                .headerCondition("X-Premium", AssertAction.EQUALS, "true")
                .respond(200, Map.of(
                        "id", "$.pathParam.id",
                        "type", "premium",
                        "extraData", Map.of("discount", "20%", "priority", "high")
                ))
                .build();

        client.mocks().create(catchAll);
        client.mocks().create(specific);
        System.out.println("Created priority-based mocks");
    }
}
