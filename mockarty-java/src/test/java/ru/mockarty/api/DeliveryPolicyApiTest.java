// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliveryPolicyApiTest {
    @Test
    void environmentPathCarriesConfiguredNamespace() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test").namespace("team a").build()) {
            DeliveryPolicyApi api = client.deliveryPolicy();
            var method = DeliveryPolicyApi.class.getDeclaredMethod("path", String.class);
            method.setAccessible(true);
            assertEquals("/api/v1/admin/delivery-policy/environments/staging?namespace=team%20a",
                    method.invoke(api, "staging"));
        }
    }
}
