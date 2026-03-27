// Copyright (c) 2026 Mockarty. All rights reserved.
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
 * SSE (Server-Sent Events) mock examples covering event streams,
 * event chains, conditional events, and real-time notification patterns.
 */
public class SseMocksExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            createSimpleSseEvent(client);
            createSseEventChain(client);
            createSseWithConditions(client);
            createSseNotificationStream(client);
            createSseProgressUpdates(client);
        }
    }

    /**
     * Simple SSE event mock.
     * Emits a single event when the path is requested.
     */
    static void createSimpleSseEvent(MockartyClient client) {
        Mock mock = MockBuilder.sse("/events/notifications", "notification")
                .id("sse-simple-notification")
                .respond(200, Map.of(
                        "id", "$.fake.UUID",
                        "type", "info",
                        "message", "New notification received",
                        "timestamp", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created simple SSE event mock");
    }

    /**
     * SSE event chain -- emits a sequence of events over time.
     * Simulates a build process with step-by-step status updates.
     */
    static void createSseEventChain(MockartyClient client) {
        Mock mock = MockBuilder.sse("/events/build-status", "build")
                .id("sse-build-chain")
                .respond(new ContentResponse()
                        .statusCode(200)
                        .sseEventChain(Map.of(
                                "events", List.of(
                                        Map.of(
                                                "event", "build",
                                                "data", Map.of("stage", "compile", "status", "running", "progress", 25),
                                                "delay", 0
                                        ),
                                        Map.of(
                                                "event", "build",
                                                "data", Map.of("stage", "test", "status", "running", "progress", 50),
                                                "delay", 1000
                                        ),
                                        Map.of(
                                                "event", "build",
                                                "data", Map.of("stage", "package", "status", "running", "progress", 75),
                                                "delay", 2000
                                        ),
                                        Map.of(
                                                "event", "build",
                                                "data", Map.of("stage", "deploy", "status", "completed", "progress", 100),
                                                "delay", 3000
                                        )
                                )
                        ))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created SSE event chain mock");
    }

    /**
     * SSE mock with header conditions.
     * Only emits events for authenticated connections.
     */
    static void createSseWithConditions(MockartyClient client) {
        Mock mock = MockBuilder.sse("/events/private", "update")
                .id("sse-private-events")
                .headerCondition("Authorization", AssertAction.NOT_EMPTY, null)
                .respond(200, Map.of(
                        "eventId", "$.fake.UUID",
                        "channel", "private",
                        "data", Map.of(
                                "userId", "$.fake.UUID",
                                "action", "profile_updated",
                                "timestamp", "$.fake.DateISO"
                        )
                ))
                .build();

        client.mocks().create(mock);
        System.out.println("Created SSE mock with conditions");
    }

    /**
     * SSE notification stream with multiple event types.
     * Simulates a real-time dashboard feed.
     */
    static void createSseNotificationStream(MockartyClient client) {
        // Alert event
        Mock alertEvent = MockBuilder.sse("/events/dashboard", "alert")
                .id("sse-dashboard-alert")
                .respond(200, Map.of(
                        "alertId", "$.fake.UUID",
                        "severity", "warning",
                        "title", "High CPU Usage Detected",
                        "description", "Server cpu-01 is at 95% utilization",
                        "timestamp", "$.fake.DateISO"
                ))
                .build();

        // Metric event
        Mock metricEvent = MockBuilder.sse("/events/dashboard", "metric")
                .id("sse-dashboard-metric")
                .respond(200, Map.of(
                        "metricId", "$.fake.UUID",
                        "name", "requests_per_second",
                        "value", "$.fake.IntRange(100,10000)",
                        "unit", "rps",
                        "timestamp", "$.fake.DateISO"
                ))
                .build();

        // Status event
        Mock statusEvent = MockBuilder.sse("/events/dashboard", "status")
                .id("sse-dashboard-status")
                .respond(200, Map.of(
                        "service", "api-gateway",
                        "status", "healthy",
                        "uptime", "$.fake.IntRange(1000,100000)",
                        "lastCheck", "$.fake.DateISO"
                ))
                .build();

        client.mocks().create(alertEvent);
        client.mocks().create(metricEvent);
        client.mocks().create(statusEvent);
        System.out.println("Created SSE notification stream mocks");
    }

    /**
     * SSE progress updates with ordered OneOf responses.
     * Simulates a file upload progress stream.
     */
    static void createSseProgressUpdates(MockartyClient client) {
        Mock mock = MockBuilder.sse("/events/upload-progress", "progress")
                .id("sse-upload-progress")
                .oneOfOrdered(
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("percent", 0, "status", "starting", "bytesUploaded", 0)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("percent", 25, "status", "uploading", "bytesUploaded", 2500000)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("percent", 50, "status", "uploading", "bytesUploaded", 5000000)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("percent", 75, "status", "uploading", "bytesUploaded", 7500000)),
                        new ContentResponse()
                                .statusCode(200)
                                .payload(Map.of("percent", 100, "status", "completed", "bytesUploaded", 10000000))
                )
                .build();

        client.mocks().create(mock);
        System.out.println("Created SSE progress update mock");
    }
}
