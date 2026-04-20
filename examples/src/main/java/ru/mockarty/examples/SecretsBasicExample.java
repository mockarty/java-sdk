// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

import java.util.List;
import java.util.Map;

/**
 * Secrets Storage — create a store, write/read/rotate/delete entries.
 */
public class SecretsBasicExample {

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            Map<String, Object> store = client.secrets().createStore(
                    "payments",
                    "API keys for payment providers",
                    "software");
            String storeId = (String) store.get("id");
            System.out.printf("[1] store=%s id=%s%n", store.get("name"), storeId);

            try {
                client.secrets().createEntry(storeId, "stripe_api_key",
                        "sk_test_abc123", "Stripe test-mode key");
                System.out.println("[2] entry created");

                Map<String, Object> entry = client.secrets().getEntry(storeId, "stripe_api_key");
                System.out.printf("[3] v%s value length=%d%n",
                        entry.get("version"),
                        String.valueOf(entry.get("value")).length());

                Map<String, Object> rotated = client.secrets().rotateEntry(storeId, "stripe_api_key");
                System.out.printf("[4] rotated to v%s%n", rotated.get("version"));

                List<Map<String, Object>> entries = client.secrets().listEntries(storeId);
                System.out.printf("[5] %d entries in store%n", entries.size());
            } finally {
                client.secrets().deleteStore(storeId);
                System.out.println("[6] store cleaned up");
            }
        }
    }
}
