// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Customer-authorized Cloud loyalty, support and risk-appeal API. */
public class CloudCustomerApi {
    private final MockartyClient client;

    public CloudCustomerApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> listLoyaltyRedemptions(String spaceId, String cursor, int limit) throws MockartyException {
        return map(client.get(spacePath(spaceId) + "/loyalty/redemptions" + pageQuery("", cursor, limit), Map.class));
    }

    public Map<String, Object> redeemLoyalty(String spaceId, String code, String region, String idempotencyKey) throws MockartyException {
        return map(client.post(spacePath(spaceId) + "/loyalty/redemptions",
                Map.of("code", code, "region", region, "idempotency_key", require(idempotencyKey, "idempotency key")), Map.class));
    }

    public Map<String, Object> listSupportCases(String spaceId, String status, String cursor, int limit) throws MockartyException {
        return map(client.get(spacePath(spaceId) + "/support/cases" + pageQuery(status, cursor, limit), Map.class));
    }

    public Map<String, Object> openSupportCase(String spaceId, String subject, String category, String priority,
                                                String message, String idempotencyKey) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subject", subject);
        body.put("category", category);
        body.put("priority", priority);
        body.put("message", message);
        body.put("idempotency_key", require(idempotencyKey, "idempotency key"));
        return map(client.post(spacePath(spaceId) + "/support/cases", body, Map.class));
    }

    public Map<String, Object> getSupportCase(String spaceId, String caseId) throws MockartyException {
        return map(client.get(spacePath(spaceId) + "/support/cases/" + encode(require(caseId, "case id")), Map.class));
    }

    public Map<String, Object> replySupportCase(String spaceId, String caseId, String body,
                                                 String idempotencyKey) throws MockartyException {
        String path = spacePath(spaceId) + "/support/cases/" + encode(require(caseId, "case id")) + "/messages";
        return map(client.post(path, Map.of("body", body, "visibility", "customer",
                "idempotency_key", require(idempotencyKey, "idempotency key")), Map.class));
    }

    public Map<String, Object> getRiskAppeal(String caseId) throws MockartyException {
        return map(client.get("/api/v1/cloud/risk/cases/" + encode(require(caseId, "case id")) + "/appeal", Map.class));
    }

    public Map<String, Object> submitRiskAppeal(String caseId, String reason, String idempotencyKey) throws MockartyException {
        String path = "/api/v1/cloud/risk/cases/" + encode(require(caseId, "case id")) + "/appeal";
        return map(client.postWithHeaders(path, Map.of("reason", reason), Map.class,
                Map.of("Idempotency-Key", require(idempotencyKey, "idempotency key"))));
    }

    static String pageQuery(String status, String cursor, int limit) {
        StringBuilder query = new StringBuilder();
        if (cursor != null && !cursor.isBlank()) append(query, "cursor", cursor);
        if (limit > 0) append(query, "limit", Integer.toString(limit));
        if (status != null && !status.isBlank()) append(query, "status", status);
        return query.length() == 0 ? "" : "?" + query;
    }

    private static void append(StringBuilder query, String name, String value) {
        if (query.length() > 0) query.append('&');
        query.append(name).append('=').append(encode(value));
    }

    private static String spacePath(String spaceId) {
        return "/api/v1/cloud/spaces/" + encode(require(spaceId, "Space id"));
    }

    static String require(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        return value;
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
