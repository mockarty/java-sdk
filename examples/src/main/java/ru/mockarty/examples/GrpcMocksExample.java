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
 * gRPC mock examples covering service/method matching,
 * metadata conditions, error simulation, and streaming patterns.
 */
public class GrpcMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleUnaryMock(client);
            createUnaryWithConditions(client);
            createUnaryWithMetadataConditions(client);
            createGrpcErrorMock(client);
            createGrpcOneOfMock(client);
            createGrpcWithFaker(client);
            createGrpcWithDelay(client);
        }
    }

    /**
     * Simple unary gRPC mock for a UserService.GetUser method.
     */
    static void createSimpleUnaryMock(MockartyClient client) {
        Mock mock = MockBuilder.grpc("user.UserService", "GetUser")
                .id("grpc-get-user")
                .respond(200, Map.of(
                        "userId", "user-123",
                        "firstName", "John",
                        "lastName", "Doe",
                        "email", "john.doe@example.com",
                        "role", "ADMIN"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple gRPC unary mock");
    }

    /**
     * gRPC mock with body field conditions.
     * Matches when request contains specific field values.
     */
    static void createUnaryWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.grpc("order.OrderService", "CreateOrder")
                .id("grpc-create-order")
                .condition("userId", AssertAction.NOT_EMPTY, null)
                .condition("items", AssertAction.NOT_EMPTY, null)
                .condition("currency", AssertAction.EQUALS, "USD")
                .respond(200, Map.of(
                        "orderId", "$.fake.UUID",
                        "userId", "$.req.userId",
                        "status", "CREATED",
                        "totalAmount", "$.fake.FloatRange(10.00,500.00)",
                        "currency", "USD",
                        "createdAt", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC mock with body conditions");
    }

    /**
     * gRPC mock with metadata (header) conditions.
     * Metadata in gRPC is analogous to HTTP headers.
     */
    static void createUnaryWithMetadataConditions(MockartyClient client) {
        Mock mock = MockBuilder.grpc("inventory.InventoryService", "CheckStock")
                .id("grpc-check-stock-auth")
                .headerCondition("authorization", AssertAction.MATCHES, "^Bearer .+$")
                .headerCondition("x-request-id", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "productId", "$.req.productId",
                        "available", true,
                        "quantity", 42,
                        "warehouse", "WH-East-1",
                        "lastChecked", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC mock with metadata conditions");
    }

    /**
     * gRPC error simulation -- returns a gRPC-style error response.
     */
    static void createGrpcErrorMock(MockartyClient client) {
        // NOT_FOUND error
        Mock notFound = MockBuilder.grpc("user.UserService", "GetUser")
                .id("grpc-user-not-found")
                .condition("userId", AssertAction.EQUALS, "nonexistent")
                .priority(100)  // Higher priority than the success mock
                .respondWithError(404, "User not found")
                .build();

        // PERMISSION_DENIED error
        Mock denied = MockBuilder.grpc("admin.AdminService", "DeleteUser")
                .id("grpc-permission-denied")
                .respondWithError(403, "Permission denied: admin role required")
                .build();

        // UNAVAILABLE error with delay (simulate downstream timeout)
        Mock unavailable = MockBuilder.grpc("payment.PaymentService", "ProcessPayment")
                .id("grpc-service-unavailable")
                .respondWithDelay(503,
                        Map.of("error", "Payment gateway temporarily unavailable"),
                        3000  // 3-second delay before error
                )
                .build();

        client.mocks().create(notFound);
        client.mocks().create(denied);
        client.mocks().create(unavailable);
        System.out.println("Created gRPC error mocks");
    }

    /**
     * gRPC OneOf mock -- returns different responses in sequence.
     * Useful for testing retry logic.
     */
    static void createGrpcOneOfMock(MockartyClient client) {
        Mock mock = MockBuilder.grpc("notification.NotificationService", "SendNotification")
                .id("grpc-oneof-retry")
                .oneOfOrdered(
                        // First call: temporarily unavailable
                        new ContentResponse()
                                .statusCode(503)
                                .error("Service temporarily unavailable"),
                        // Second call: deadline exceeded
                        new ContentResponse()
                                .statusCode(504)
                                .error("Deadline exceeded")
                                .delay(5000),
                        // Third call: success
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of(
                                        "notificationId", "$.fake.UUID",
                                        "status", "SENT",
                                        "deliveredAt", "$.fake.DateISO"
                                ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC OneOf mock for retry testing");
    }

    /**
     * gRPC mock with Faker-populated fields for realistic test data.
     */
    static void createGrpcWithFaker(MockartyClient client) {
        Mock mock = MockBuilder.grpc("product.ProductService", "ListProducts")
                .id("grpc-list-products-faker")
                .respond(200, Map.of(
                        "products", List.of(
                                Map.of(
                                        "productId", "$.fake.UUID",
                                        "name", "$.fake.Word",
                                        "description", "$.fake.Sentence",
                                        "price", "$.fake.FloatRange(5.99,299.99)",
                                        "currency", "USD",
                                        "inStock", true
                                ),
                                Map.of(
                                        "productId", "$.fake.UUID",
                                        "name", "$.fake.Word",
                                        "description", "$.fake.Sentence",
                                        "price", "$.fake.FloatRange(5.99,299.99)",
                                        "currency", "USD",
                                        "inStock", false
                                )
                        ),
                        "totalCount", 2,
                        "pageToken", "$.fake.UUID"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC mock with Faker data");
    }

    /**
     * gRPC mock with delay for timeout testing.
     */
    static void createGrpcWithDelay(MockartyClient client) {
        Mock mock = MockBuilder.grpc("analytics.AnalyticsService", "GenerateReport")
                .id("grpc-slow-report")
                .respondWithDelay(200,
                        Map.of(
                                "reportId", "$.fake.UUID",
                                "status", "COMPLETED",
                                "rows", 15000,
                                "generatedAt", "$.fake.DateISO"
                        ),
                        4000  // 4-second delay
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created gRPC mock with 4s delay");
    }
}
