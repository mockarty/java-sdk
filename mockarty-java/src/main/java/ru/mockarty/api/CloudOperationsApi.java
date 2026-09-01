// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.util.Map;

/** Least-privilege operator support and product analytics API. */
public class CloudOperationsApi {
    private static final String SUPPORT = "/api/v1/cloud/operator/support/cases";
    private final MockartyClient client;

    public CloudOperationsApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> listSupportCases(String status, String cursor, int limit) throws MockartyException {
        return map(client.get(SUPPORT + CloudCustomerApi.pageQuery(status, cursor, limit), Map.class));
    }

    public Map<String, Object> getSupportCase(String caseId) throws MockartyException {
        return map(client.get(casePath(caseId), Map.class));
    }

    public Map<String, Object> replySupportCase(String caseId, String body, String visibility,
                                                 String idempotencyKey) throws MockartyException {
        return map(client.post(casePath(caseId) + "/messages", Map.of("body", body, "visibility", visibility,
                "idempotency_key", CloudCustomerApi.require(idempotencyKey, "idempotency key")), Map.class));
    }

    public Map<String, Object> assignSupportCase(String caseId, String assigneeUserId,
                                                  long expectedGeneration) throws MockartyException {
        return map(client.post(casePath(caseId) + "/assign", Map.of("assignee_user_id", assigneeUserId,
                "expected_generation", expectedGeneration), Map.class));
    }

    public Map<String, Object> transitionSupportCase(String caseId, String status,
                                                      long expectedGeneration) throws MockartyException {
        return map(client.post(casePath(caseId) + "/transition", Map.of("status", status,
                "expected_generation", expectedGeneration), Map.class));
    }

    public Map<String, Object> productAnalytics(int days) throws MockartyException {
        if (days < 1 || days > 90) throw new IllegalArgumentException("days must be between 1 and 90");
        return map(client.get("/api/v1/cloud/operator/analytics/product?days=" + days, Map.class));
    }

    private static String casePath(String caseId) {
        return SUPPORT + "/" + CloudCustomerApi.encode(CloudCustomerApi.require(caseId, "case id"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
