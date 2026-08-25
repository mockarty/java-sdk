// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.Map;

/** Reads an unsigned committed Cloud entitlement projection for inspection. */
public final class CloudEntitlementsExample {
    private CloudEntitlementsExample() {}

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String spaceId = requireEnvironment("MOCKARTY_CLOUD_SPACE_ID");
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(requireEnvironment("MOCKARTY_BASE_URL"))
                .apiKey(requireEnvironment("MOCKARTY_API_KEY"))
                .build()) {
            Map<String, Object> projection = client.cloudEntitlements().get(spaceId);
            Map<String, Object> snapshot = (Map<String, Object>) projection.get("snapshot");
            System.out.printf("space=%s plan=%s revision=%s digest=%s%n",
                    spaceId, snapshot.get("plan"), projection.get("revision"), projection.get("digest"));
            // This projection is deliberately unsigned inspection data. Commercial
            // admission must use a verified SnapshotV1 authority, never this map.
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
