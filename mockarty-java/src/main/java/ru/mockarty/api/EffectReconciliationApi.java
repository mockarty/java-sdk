// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admin queue for unresolved external effects. */
public final class EffectReconciliationApi {
    private static final String QUEUE_PATH = "/api/v1/admin/effects/reconciliation";
    private final MockartyClient client;

    public EffectReconciliationApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> listQueue(String projectId, String effectFamily, String reason,
                                         long minAgeSeconds, int limit, String cursor) throws MockartyException {
        if (minAgeSeconds < 0 || minAgeSeconds > 7_776_000) {
            throw new IllegalArgumentException("effect reconciliation minAgeSeconds must be between 0 and 7776000");
        }
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("effect reconciliation limit must be between 1 and 100");
        }
        StringBuilder query = new StringBuilder("?namespace=").append(enc(client.getConfig().getNamespace()))
                .append("&minAgeSeconds=").append(minAgeSeconds).append("&limit=").append(limit);
        append(query, "project", projectId);
        append(query, "family", effectFamily);
        append(query, "reason", reason);
        append(query, "cursor", cursor);
        return client.get(QUEUE_PATH + query, Map.class);
    }

    public Map<String, Object> reconcileNoEffect(String executionId, String providerReference,
                                                  String evidenceSource) throws MockartyException {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("effect reconciliation executionId is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("namespace", client.getConfig().getNamespace());
        body.put("executionId", executionId.trim());
        body.put("decision", "no_effect");
        body.put("autoClaim", true);
        body.put("providerReference", trim(providerReference));
        body.put("evidenceSource", trim(evidenceSource));
        return client.post(QUEUE_PATH + "/reconcile", body, Map.class);
    }

    private static void append(StringBuilder query, String name, String value) {
        if (value != null && !value.isBlank()) query.append('&').append(name).append('=').append(enc(value));
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
