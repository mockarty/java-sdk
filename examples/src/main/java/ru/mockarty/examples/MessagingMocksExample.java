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
 * Messaging mock examples for Kafka, RabbitMQ, and SMTP protocols.
 * Demonstrates topic/queue matching, message conditions, and response routing.
 */
public class MessagingMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            // Kafka examples
            createKafkaConsumerMock(client);
            createKafkaWithConditions(client);
            createKafkaWithOutputTopic(client);

            // RabbitMQ examples
            createRabbitMqConsumerMock(client);
            createRabbitMqWithConditions(client);
            createRabbitMqWithOutputRouting(client);

            // SMTP examples
            createSmtpMock(client);
            createSmtpWithConditions(client);
        }
    }

    // ---- Kafka Examples ----

    /**
     * Simple Kafka consumer mock.
     * Responds to messages on the "orders" topic.
     */
    static void createKafkaConsumerMock(MockartyClient client) {
        Mock mock = MockBuilder.kafka("orders", "order-processor")
                .id("kafka-orders-consumer")
                .respond(200, Map.of(
                        "status", "processed",
                        "orderId", "$.req.orderId",
                        "processedAt", "$.fake.DateISO",
                        "correlationId", "$.fake.UUID"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created Kafka consumer mock");
    }

    /**
     * Kafka mock with message body conditions.
     * Matches only high-priority orders.
     */
    static void createKafkaWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.kafka("orders", "order-processor")
                .id("kafka-high-priority-orders")
                .priority(100)
                .condition("priority", AssertAction.EQUALS, "HIGH")
                .condition("amount", AssertAction.NOT_EMPTY, null)
                .headerCondition("X-Source", AssertAction.EQUALS, "web")
                .respond(200, Map.of(
                        "status", "express_processed",
                        "orderId", "$.req.orderId",
                        "priority", "HIGH",
                        "estimatedDelivery", "$.fake.DateISO",
                        "trackingNumber", "$.fake.UUID"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created Kafka mock with conditions");
    }

    /**
     * Kafka mock that produces to an output topic after processing.
     * Simulates a message transformer / enricher pattern.
     */
    static void createKafkaWithOutputTopic(MockartyClient client) {
        Mock mock = MockBuilder.kafka("raw-events", "event-enricher")
                .id("kafka-event-enricher")
                .respond(200, Map.of(
                        "eventId", "$.req.eventId",
                        "type", "$.req.type",
                        "enrichedData", Map.of(
                                "geoLocation", "$.fake.City",
                                "userAgent", "$.fake.UserAgent",
                                "processedAt", "$.fake.DateISO"
                        ),
                        "version", 2
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created Kafka enricher mock");
    }

    // ---- RabbitMQ Examples ----

    /**
     * Simple RabbitMQ consumer mock.
     * Responds to messages on the "notifications" queue.
     */
    static void createRabbitMqConsumerMock(MockartyClient client) {
        Mock mock = MockBuilder.rabbitmq("notifications", "notification-service")
                .id("rabbitmq-notifications")
                .respond(200, Map.of(
                        "notificationId", "$.fake.UUID",
                        "status", "delivered",
                        "channel", "email",
                        "recipient", "$.req.recipient",
                        "deliveredAt", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created RabbitMQ consumer mock");
    }

    /**
     * RabbitMQ mock with message conditions.
     * Routes based on message content.
     */
    static void createRabbitMqWithConditions(MockartyClient client) {
        // SMS notification handler
        Mock smsMock = MockBuilder.rabbitmq("notifications", "notification-service")
                .id("rabbitmq-sms-notification")
                .priority(100)
                .condition("channel", AssertAction.EQUALS, "sms")
                .condition("phoneNumber", AssertAction.MATCHES, "^\\+\\d{10,15}$")
                .respond(200, Map.of(
                        "notificationId", "$.fake.UUID",
                        "status", "sent",
                        "channel", "sms",
                        "provider", "twilio",
                        "sentAt", "$.fake.DateISO"
                ))
                .build();

        // Push notification handler
        Mock pushMock = MockBuilder.rabbitmq("notifications", "notification-service")
                .id("rabbitmq-push-notification")
                .priority(100)
                .condition("channel", AssertAction.EQUALS, "push")
                .condition("deviceToken", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "notificationId", "$.fake.UUID",
                        "status", "sent",
                        "channel", "push",
                        "provider", "fcm",
                        "sentAt", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(smsMock);
        client.mocks().create(pushMock);
        System.out.println("Created RabbitMQ conditional mocks");
    }

    /**
     * RabbitMQ mock with output routing (dead letter / result queue).
     */
    static void createRabbitMqWithOutputRouting(MockartyClient client) {
        Mock mock = MockBuilder.rabbitmq("task-queue", "task-worker")
                .id("rabbitmq-task-worker")
                .condition("taskType", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "taskId", "$.req.taskId",
                        "status", "completed",
                        "result", Map.of(
                                "output", "Task processed successfully",
                                "duration", "$.fake.IntRange(100,5000)",
                                "completedAt", "$.fake.DateISO"
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created RabbitMQ task worker mock");
    }

    // ---- SMTP Examples ----

    /**
     * Simple SMTP mock for email testing.
     * Captures all outgoing emails.
     */
    static void createSmtpMock(MockartyClient client) {
        Mock mock = MockBuilder.smtp("mail-server")
                .id("smtp-catch-all")
                .respond(200, Map.of(
                        "messageId", "$.fake.UUID",
                        "status", "accepted",
                        "queuedAt", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created SMTP catch-all mock");
    }

    /**
     * SMTP mock with sender, recipient, and subject conditions.
     * Only matches emails from specific addresses with matching subjects.
     */
    static void createSmtpWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.smtp("mail-server")
                .id("smtp-welcome-email")
                .priority(100)
                .respond(200, Map.of(
                        "messageId", "$.fake.UUID",
                        "status", "delivered",
                        "deliveredAt", "$.fake.DateISO",
                        "templateUsed", "welcome-template"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created SMTP conditional mock");
    }
}
