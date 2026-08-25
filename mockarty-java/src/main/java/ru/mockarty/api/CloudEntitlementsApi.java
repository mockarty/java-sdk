// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Read-only committed Cloud entitlement projections. */
public class CloudEntitlementsApi {
    private final MockartyClient client;

    public CloudEntitlementsApi(MockartyClient client) { this.client = client; }

    /** Returns unsigned inspection data for one explicit Space. */
    public Map<String, Object> get(String spaceId) throws MockartyException {
        if (spaceId == null || spaceId.isBlank()) {
            throw new IllegalArgumentException("Space id is required");
        }
        String encoded = URLEncoder.encode(spaceId, StandardCharsets.UTF_8).replace("+", "%20");
        return map(client.get("/api/v1/cloud/entitlements?space_id=" + encoded, Map.class));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
