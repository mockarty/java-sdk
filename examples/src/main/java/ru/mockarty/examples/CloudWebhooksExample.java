// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.CloudWebhookCredential;
import ru.mockarty.model.CloudWebhookDelivery;

import java.util.List;
import java.util.UUID;

/** Creates, verifies, inspects and safely rotates a Mockarty Cloud webhook. */
public final class CloudWebhooksExample {
    private CloudWebhooksExample() {
    }

    public static void main(String[] args) {
        String workspaceId = requireEnvironment("MOCKARTY_CLOUD_WORKSPACE_ID");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(requireEnvironment("MOCKARTY_BASE_URL"))
                .apiKey(requireEnvironment("MOCKARTY_API_KEY"))
                .build()) {
            CloudWebhookCredential created = client.cloudWebhooks().create(
                    workspaceId,
                    "Release notifications",
                    "https://hooks.example.com/mockarty",
                    List.of("instance.ready", "instance.failed", "subscription.changed"));

            String webhookId = created.getWebhook().getId();
            // Persist this value in a secret manager now: create/rotate responses show it once.
            // Never write signing secrets to application logs.
            String signingSecret = created.getSecret();
            System.out.println("Signing secret received: " + !signingSecret.isBlank());

            client.cloudWebhooks().test(workspaceId, webhookId);
            for (CloudWebhookDelivery delivery
                    : client.cloudWebhooks().listDeliveries(workspaceId, webhookId, 25)) {
                System.out.printf("%s %s attempt=%d statusCode=%s%n",
                        delivery.getEvent(), delivery.getStatus(), delivery.getAttempt(),
                        delivery.getStatusCode());
            }

            CloudWebhookCredential rotated = client.cloudWebhooks().rotateSecret(
                    workspaceId, webhookId, UUID.randomUUID().toString());
            String replacementSigningSecret = rotated.getSecret();
            System.out.println("Replacement signing secret received: "
                    + !replacementSigningSecret.isBlank());

            // Deactivation preserves the delivery history for later investigation.
            // client.cloudWebhooks().deactivate(workspaceId, webhookId);
        }
    }

    private static String requireEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
