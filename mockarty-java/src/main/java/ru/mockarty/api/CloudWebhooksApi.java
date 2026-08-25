// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CloudWebhook;
import ru.mockarty.model.CloudWebhookCredential;
import ru.mockarty.model.CloudWebhookDelivery;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Curated workspace webhook automation for Mockarty Cloud. */
public class CloudWebhooksApi {
    private static final int DEFAULT_DELIVERY_LIMIT = 100;
    private static final int MAX_DELIVERY_LIMIT = 500;

    private final MockartyClient client;

    public CloudWebhooksApi(MockartyClient client) {
        this.client = client;
    }

    /** Lists active webhooks visible in the selected workspace. */
    public List<CloudWebhook> list(String workspaceId) throws MockartyException {
        CloudWebhookList response = client.get(collectionPath(workspaceId), CloudWebhookList.class);
        return response == null || response.webhooks == null
                ? Collections.emptyList() : response.webhooks;
    }

    /** Creates a webhook and returns its signing secret once. */
    public CloudWebhookCredential create(String workspaceId, String name, String targetUrl,
                                         List<String> events) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("url", targetUrl);
        body.put("events", events == null ? Collections.emptyList() : events);
        return client.post(collectionPath(workspaceId), body, CloudWebhookCredential.class);
    }

    /** Deactivates a webhook while retaining its delivery history. */
    public void deactivate(String workspaceId, String webhookId) throws MockartyException {
        client.delete(webhookPath(workspaceId, requireWebhookId(webhookId)));
    }

    /** Queues a signed synthetic test event for one webhook. */
    public void test(String workspaceId, String webhookId) throws MockartyException {
        client.post(webhookActionPath(workspaceId, requireWebhookId(webhookId), "test"),
                Collections.emptyMap());
    }

    /** Lists a bounded delivery history for one webhook. */
    public List<CloudWebhookDelivery> listDeliveries(String workspaceId, String webhookId, int limit)
            throws MockartyException {
        int boundedLimit = limit <= 0 || limit > MAX_DELIVERY_LIMIT ? DEFAULT_DELIVERY_LIMIT : limit;
        String path = webhookActionPath(workspaceId, requireWebhookId(webhookId), "deliveries")
                + "&limit=" + boundedLimit;
        CloudWebhookDeliveryList response = client.get(path, CloudWebhookDeliveryList.class);
        return response == null || response.deliveries == null
                ? Collections.emptyList() : response.deliveries;
    }

    /**
     * Rotates a signing secret and returns the new value once. Reuse the same
     * idempotency key when retrying an ambiguous request.
     */
    public CloudWebhookCredential rotateSecret(String workspaceId, String webhookId,
                                                String idempotencyKey) throws MockartyException {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotency key is required");
        }
        return client.postWithHeaders(
                webhookActionPath(workspaceId, requireWebhookId(webhookId), "rotate-secret"),
                Collections.emptyMap(), CloudWebhookCredential.class,
                Map.of("Idempotency-Key", idempotencyKey));
    }

    private static String collectionPath(String workspaceId) {
        return "/api/v1/cloud/webhooks?workspace_id=" + encode(workspaceId == null ? "" : workspaceId);
    }

    private static String webhookPath(String workspaceId, String webhookId) {
        return "/api/v1/cloud/webhooks/" + encode(webhookId)
                + "?workspace_id=" + encode(workspaceId == null ? "" : workspaceId);
    }

    private static String webhookActionPath(String workspaceId, String webhookId, String action) {
        return "/api/v1/cloud/webhooks/" + encode(webhookId) + "/" + action
                + "?workspace_id=" + encode(workspaceId == null ? "" : workspaceId);
    }

    private static String requireWebhookId(String webhookId) {
        if (webhookId == null || webhookId.isBlank()) {
            throw new IllegalArgumentException("webhook id is required");
        }
        return webhookId;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CloudWebhookList {
        private List<CloudWebhook> webhooks;

        public void setWebhooks(List<CloudWebhook> webhooks) {
            this.webhooks = webhooks;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class CloudWebhookDeliveryList {
        private List<CloudWebhookDelivery> deliveries;

        public void setDeliveries(List<CloudWebhookDelivery> deliveries) {
            this.deliveries = deliveries;
        }
    }
}
