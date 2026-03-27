// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.CleanupPolicy;
import ru.mockarty.model.NamespaceUser;

import java.util.List;
import java.util.Map;

/**
 * Namespace settings examples demonstrating user management,
 * cleanup policies, and webhook configuration per namespace.
 *
 * <p>Namespace settings control:</p>
 * <ul>
 *   <li>Users and roles within a namespace</li>
 *   <li>Automatic cleanup policies for expired mocks and logs</li>
 *   <li>Webhooks for event notifications</li>
 * </ul>
 */
public class NamespaceSettingsExample {

    private static final String NAMESPACE = "sandbox";

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace(NAMESPACE)
                .build()) {

            userManagement(client);
            cleanupPolicies(client);
            webhookManagement(client);
        }
    }

    /**
     * Manage users and roles within a namespace.
     */
    static void userManagement(MockartyClient client) {
        System.out.println("=== User Management ===");

        // List users in the namespace
        List<NamespaceUser> users = client.namespaceSettings().listUsers(NAMESPACE);
        System.out.println("Users in '" + NAMESPACE + "': " + users.size());
        for (NamespaceUser user : users) {
            System.out.println("  " + user.getUserId() + " - Role: " + user.getRole());
        }

        // Add a user to the namespace with a specific role
        client.namespaceSettings().addUser(NAMESPACE, Map.of(
                "userId", "user-john-doe",
                "role", "editor"
        ));
        System.out.println("Added user 'user-john-doe' as editor");

        // Add another user as viewer
        client.namespaceSettings().addUser(NAMESPACE, Map.of(
                "userId", "user-jane-doe",
                "role", "viewer"
        ));
        System.out.println("Added user 'user-jane-doe' as viewer");

        // Update user role (promote to admin)
        client.namespaceSettings().updateUserRole(NAMESPACE, "user-john-doe", "admin");
        System.out.println("Updated 'user-john-doe' role to admin");

        // Verify the change
        users = client.namespaceSettings().listUsers(NAMESPACE);
        for (NamespaceUser user : users) {
            System.out.println("  " + user.getUserId() + " - Role: " + user.getRole());
        }

        // Remove a user from the namespace
        client.namespaceSettings().removeUser(NAMESPACE, "user-jane-doe");
        System.out.println("Removed 'user-jane-doe' from namespace");
    }

    /**
     * Configure cleanup policies for automatic resource management.
     */
    static void cleanupPolicies(MockartyClient client) {
        System.out.println("\n=== Cleanup Policies ===");

        // Get the current cleanup policy
        CleanupPolicy current = client.namespaceSettings().getCleanupPolicy(NAMESPACE);
        System.out.println("Current cleanup policy:");
        System.out.println("  Enabled: " + current.isEnabled());
        System.out.println("  Mock TTL: " + current.getMockTtlDays() + " days");
        System.out.println("  Log retention: " + current.getLogRetentionDays() + " days");

        // Update the cleanup policy
        CleanupPolicy updated = new CleanupPolicy()
                .enabled(true)
                .mockTtlDays(30)           // Delete mocks older than 30 days
                .logRetentionDays(7)       // Keep logs for 7 days
                .cleanupDeletedMocks(true) // Purge soft-deleted mocks
                .cleanupExpiredMocks(true) // Remove expired TTL mocks
                .cleanupInterval("24h");   // Run cleanup every 24 hours

        client.namespaceSettings().updateCleanupPolicy(NAMESPACE, updated);
        System.out.println("Updated cleanup policy:");
        System.out.println("  Mock TTL: 30 days");
        System.out.println("  Log retention: 7 days");
        System.out.println("  Cleanup interval: 24h");

        // Verify the update
        CleanupPolicy verified = client.namespaceSettings().getCleanupPolicy(NAMESPACE);
        System.out.println("Verified - Enabled: " + verified.isEnabled());
    }

    /**
     * Configure webhooks for namespace event notifications.
     */
    static void webhookManagement(MockartyClient client) {
        System.out.println("\n=== Webhook Management ===");

        // List existing webhooks
        List<Map<String, Object>> webhooks = client.namespaceSettings().listWebhooks(NAMESPACE);
        System.out.println("Existing webhooks: " + webhooks.size());

        // Create a webhook for mock creation events
        Map<String, Object> mockWebhook = client.namespaceSettings().createWebhook(NAMESPACE, Map.of(
                "name", "Mock Changes Notification",
                "url", "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXX",
                "events", List.of("mock.created", "mock.updated", "mock.deleted"),
                "headers", Map.of(
                        "Content-Type", "application/json"
                ),
                "enabled", true
        ));
        System.out.println("Created webhook: " + mockWebhook.get("id"));

        // Create a webhook for test run events
        Map<String, Object> testWebhook = client.namespaceSettings().createWebhook(NAMESPACE, Map.of(
                "name", "Test Run Results",
                "url", "https://api.pagerduty.com/events/v2/enqueue",
                "events", List.of("testrun.completed", "testrun.failed"),
                "headers", Map.of(
                        "Content-Type", "application/json",
                        "Authorization", "Token token=pagerduty-key"
                ),
                "enabled", true
        ));
        System.out.println("Created webhook: " + testWebhook.get("id"));

        // Create a webhook for fuzzing findings
        Map<String, Object> fuzzingWebhook = client.namespaceSettings().createWebhook(NAMESPACE, Map.of(
                "name", "Security Findings Alert",
                "url", "https://hooks.slack.com/services/T00000000/SECURITY/CHANNEL",
                "events", List.of("fuzzing.finding.critical", "fuzzing.finding.high"),
                "enabled", true
        ));
        System.out.println("Created webhook: " + fuzzingWebhook.get("id"));

        // List all webhooks again
        webhooks = client.namespaceSettings().listWebhooks(NAMESPACE);
        System.out.println("Total webhooks: " + webhooks.size());
        for (Map<String, Object> wh : webhooks) {
            System.out.println("  " + wh.get("name") + " -> " + wh.get("url"));
            System.out.println("    Events: " + wh.get("events"));
            System.out.println("    Enabled: " + wh.get("enabled"));
        }

        // Delete a webhook
        // client.namespaceSettings().deleteWebhook(NAMESPACE, mockWebhook.get("id").toString());
        // System.out.println("Deleted webhook");
    }
}
