// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.Callback;
import ru.mockarty.model.Mock;

import java.util.Map;

/**
 * Callback (webhook) examples showing HTTP, Kafka, and RabbitMQ callbacks
 * that fire when a mock is matched.
 */
public class CallbacksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createMockWithHttpCallback(client);
            createMockWithKafkaCallback(client);
            createMockWithRabbitMqCallback(client);
            createMockWithAsyncCallback(client);
            createMockWithRetryCallback(client);
            createMockWithMultipleCallbacks(client);
        }
    }

    /**
     * Mock with an HTTP webhook callback.
     * Notifies an external URL when a new order is created.
     */
    static void createMockWithHttpCallback(MockartyClient client) {
        Mock mock = MockBuilder.http("/api/orders", "POST")
                .id("callback-http-order")
                .callback(
                        "https://webhook.example.com/order-created",
                        "POST",
                        Map.of(
                                "event", "order.created",
                                "orderId", "$.fake.UUID",
                                "timestamp", "$.fake.DateISO"
                        )
                )
                .respond(201, Map.of(
                        "orderId", "$.fake.UUID",
                        "status", "created"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with HTTP callback");
    }

    /**
     * Mock with a Kafka callback.
     * Publishes a message to a Kafka topic when the mock is matched.
     */
    static void createMockWithKafkaCallback(MockartyClient client) {
        Callback kafkaCallback = Callback.kafka(
                "localhost:9092",
                "order-events",
                Map.of(
                        "event", "order.payment_received",
                        "orderId", "$.req.orderId",
                        "amount", "$.req.amount",
                        "timestamp", "$.fake.DateISO"
                )
        );
        kafkaCallback.kafkaKey("$.req.orderId");

        Mock mock = MockBuilder.http("/api/payments", "POST")
                .id("callback-kafka-payment")
                .callback(kafkaCallback)
                .respond(200, Map.of(
                        "paymentId", "$.fake.UUID",
                        "status", "processed"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with Kafka callback");
    }

    /**
     * Mock with a RabbitMQ callback.
     * Publishes a message to a RabbitMQ exchange when matched.
     */
    static void createMockWithRabbitMqCallback(MockartyClient client) {
        Callback rabbitCallback = Callback.rabbitmq(
                "amqp://guest:guest@localhost:5672/",
                "events",
                "user.registered",
                Map.of(
                        "event", "user.registered",
                        "userId", "$.fake.UUID",
                        "email", "$.req.email",
                        "registeredAt", "$.fake.DateISO"
                )
        );

        Mock mock = MockBuilder.http("/api/users/register", "POST")
                .id("callback-rabbitmq-register")
                .callback(rabbitCallback)
                .respond(201, Map.of(
                        "userId", "$.fake.UUID",
                        "message", "Registration successful"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with RabbitMQ callback");
    }

    /**
     * Mock with an async callback that fires independently.
     * The mock responds immediately; the callback fires in the background.
     */
    static void createMockWithAsyncCallback(MockartyClient client) {
        Callback asyncCallback = Callback.http(
                "https://analytics.example.com/events",
                "POST",
                Map.of(
                        "event", "api.call",
                        "endpoint", "/api/products",
                        "method", "GET",
                        "timestamp", "$.fake.DateISO"
                )
        );
        asyncCallback.async(true);
        asyncCallback.timeout(5000);

        Mock mock = MockBuilder.http("/api/products", "GET")
                .id("callback-async-analytics")
                .callback(asyncCallback)
                .respond(200, Map.of(
                        "products", java.util.List.of(
                                Map.of("id", "$.fake.UUID", "name", "$.fake.Word", "price", "$.fake.FloatRange(1.00,99.99)")
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with async callback");
    }

    /**
     * Mock with a callback that has retry logic.
     * Retries up to 3 times with 2-second delays on failure.
     */
    static void createMockWithRetryCallback(MockartyClient client) {
        Callback retryCallback = Callback.http(
                "https://billing.example.com/invoice",
                "POST",
                Map.of(
                        "invoiceId", "$.fake.UUID",
                        "orderId", "$.req.orderId",
                        "amount", "$.req.amount",
                        "currency", "USD"
                )
        );
        retryCallback.retryCount(3);
        retryCallback.retryDelay(2000);
        retryCallback.timeout(10000);
        retryCallback.headers(Map.of(
                "Authorization", "Bearer billing-api-key",
                "Content-Type", "application/json"
        ));

        Mock mock = MockBuilder.http("/api/orders/:id/complete", "POST")
                .id("callback-retry-billing")
                .callback(retryCallback)
                .respond(200, Map.of(
                        "orderId", "$.pathParam.id",
                        "status", "completed"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with retry callback");
    }

    /**
     * Mock with multiple callbacks of different types.
     * Fires HTTP, Kafka, and RabbitMQ callbacks simultaneously.
     */
    static void createMockWithMultipleCallbacks(MockartyClient client) {
        // HTTP callback: notify admin dashboard
        Callback httpNotify = Callback.http(
                "https://admin.example.com/alerts",
                "POST",
                Map.of("alert", "critical_order", "orderId", "$.req.orderId")
        );
        httpNotify.async(true);

        // Kafka callback: publish to audit trail
        Callback kafkaAudit = Callback.kafka(
                "localhost:9092",
                "audit-trail",
                Map.of(
                        "action", "high_value_order",
                        "orderId", "$.req.orderId",
                        "amount", "$.req.amount"
                )
        );

        // RabbitMQ callback: trigger fraud check
        Callback rabbitFraud = Callback.rabbitmq(
                "amqp://guest:guest@localhost:5672/",
                "fraud-checks",
                "order.check",
                Map.of(
                        "orderId", "$.req.orderId",
                        "amount", "$.req.amount",
                        "checkType", "high_value"
                )
        );

        Mock mock = MockBuilder.http("/api/orders/high-value", "POST")
                .id("callback-multi-channel")
                .callback(httpNotify)
                .callback(kafkaAudit)
                .callback(rabbitFraud)
                .respond(201, Map.of(
                        "orderId", "$.fake.UUID",
                        "status", "pending_review",
                        "message", "High-value order created, awaiting review"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created mock with multiple callbacks");
    }
}
