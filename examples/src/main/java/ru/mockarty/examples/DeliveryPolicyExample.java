// Copyright (c) 2026 Mockarty. All rights reserved.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.Map;

public final class DeliveryPolicyExample {
    private DeliveryPolicyExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            Map<String, Object> environment = client.deliveryPolicy().create(Map.of(
                    "id", "staging",
                    "projectId", "payments",
                    "class", "staging",
                    "profile", "standard",
                    "auditId", "change-123",
                    "evidenceId", "review-123"
            ), "payments-staging-v1");
            System.out.println(environment);
        }
    }
}
