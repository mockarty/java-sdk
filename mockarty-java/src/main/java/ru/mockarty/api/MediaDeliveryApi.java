// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Operator reconciliation for media jobs held after ambiguous runner delivery. */
public final class MediaDeliveryApi {
    private final MockartyClient client;

    public MediaDeliveryApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> listFenced(String engine) throws MockartyException {
        return client.get(base(engine) + "/fenced" + namespaceQuery(), Map.class);
    }

    public void reconcile(String engine, String jobId, String runnerId, String outcome) throws MockartyException {
        require(jobId, "job id");
        require(runnerId, "runner id");
        if (!"not_started".equals(outcome) && !"started".equals(outcome)) {
            throw new IllegalArgumentException("media delivery outcome must be not_started or started");
        }
        client.post(base(engine) + "/" + encode(jobId) + "/reconcile-delivery" + namespaceQuery(),
                Map.of("runnerId", runnerId, "outcome", outcome), Map.class);
    }

    private String base(String engine) {
        if (!"transcribe".equals(engine) && !"tts".equals(engine)) {
            throw new IllegalArgumentException("media delivery engine must be transcribe or tts");
        }
        return "/api/v1/" + engine + "/jobs";
    }

    private String namespaceQuery() {
        return "?namespace=" + encode(client.getConfig().getNamespace());
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("media delivery " + name + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
