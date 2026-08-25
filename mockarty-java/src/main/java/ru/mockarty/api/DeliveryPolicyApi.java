// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Administrator delivery-policy environment management. */
public final class DeliveryPolicyApi {
    private static final String BASE = "/api/v1/admin/delivery-policy/environments";
    private final MockartyClient client;

    public DeliveryPolicyApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> create(Map<String, Object> body, String idempotencyKey) throws MockartyException {
        return map(client.postWithHeaders(collectionPath(), body, Map.class,
                Map.of("Idempotency-Key", require(idempotencyKey, "idempotency key"))));
    }

    public Map<String, Object> get(String environmentId) throws MockartyException {
        return map(client.get(path(environmentId), Map.class));
    }

    public Map<String, Object> list(String status, String cursor, int limit) throws MockartyException {
        StringBuilder query = new StringBuilder();
        appendQuery(query, "namespace", client.getConfig().getNamespace());
        appendQuery(query, "status", status);
        appendQuery(query, "cursor", cursor);
        if (limit > 0) appendQuery(query, "limit", Integer.toString(limit));
        return map(client.get(BASE + query, Map.class));
    }

    public Map<String, Object> advance(String environmentId, Map<String, Object> body,
                                        String etag, String idempotencyKey) throws MockartyException {
        Map<String, Object> payload = new LinkedHashMap<>(body == null ? Map.of() : body);
        payload.remove("id");
        return map(client.putWithHeaders(path(environmentId), payload, Map.class,
                Map.of("If-Match", require(etag, "ETag"),
                        "Idempotency-Key", require(idempotencyKey, "idempotency key"))));
    }

    public void revoke(String environmentId, String etag) throws MockartyException {
        client.deleteWithHeaders(path(environmentId), Map.of("If-Match", require(etag, "ETag")));
    }

    private String collectionPath() {
        StringBuilder query = new StringBuilder();
        appendQuery(query, "namespace", client.getConfig().getNamespace());
        return BASE + query;
    }

    private String path(String environmentId) {
        StringBuilder query = new StringBuilder();
        appendQuery(query, "namespace", client.getConfig().getNamespace());
        return BASE + "/" + encode(require(environmentId, "environment id")) + query;
    }

    private static void appendQuery(StringBuilder query, String key, String value) {
        if (value == null || value.isBlank()) return;
        query.append(query.length() == 0 ? '?' : '&').append(key).append('=').append(encode(value));
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("delivery-policy " + name + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
