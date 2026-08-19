// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.builder.UITestBuilder;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Recorded UI test API — save / run / poll / export. The SDK orchestrates
 * execution on the platform's browser-runner / companion; it never embeds a
 * browser. Author a {@link UITestBuilder} (or generate one from a recording),
 * {@code create} it, {@code run} it, and poll the result.
 */
public class UITestApi {

    private static final java.util.Set<String> TERMINAL = java.util.Set.of(
            "passed", "failed", "broken", "skipped", "cancelled", "error", "completed");

    private final MockartyClient client;

    public UITestApi(MockartyClient client) {
        this.client = client;
    }

    private String ns() {
        String namespace = client.getConfig().getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            return "";
        }
        return "?namespace=" + URLEncoder.encode(namespace, StandardCharsets.UTF_8);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Save a UI test (POST /api/v1/ui-tests). Returns the stored record. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(UITestBuilder ui) throws MockartyException {
        return client.post("/api/v1/ui-tests" + ns(), ui.toMap(), Map.class);
    }

    /** List saved UI tests in the namespace. */
    public List<Map<String, Object>> list() throws MockartyException {
        JsonNode root = client.get("/api/v1/ui-tests" + ns(), JsonNode.class);
        JsonNode arr = root != null && root.isObject() ? root.path("uiTests") : root;
        List<Map<String, Object>> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            JavaType t = client.getObjectMapper().getTypeFactory()
                    .constructMapType(java.util.LinkedHashMap.class, String.class, Object.class);
            for (JsonNode n : arr) {
                out.add(client.getObjectMapper().convertValue(n, t));
            }
        }
        return out;
    }

    /** Get a saved UI test by id. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String id) throws MockartyException {
        return client.get("/api/v1/ui-tests/" + enc(id) + ns(), Map.class);
    }

    /**
     * Dispatch a replay on a runner. Returns {@code {taskId, statusPath, ...}}.
     * {@code options} may carry browser / viewport / envVars / platform / etc.
     * (pass null or an empty map to replay as captured).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(String id, Map<String, Object> options) throws MockartyException {
        Object body = options != null ? options : Map.of();
        return client.post("/api/v1/ui-tests/" + enc(id) + "/run" + ns(), body, Map.class);
    }

    /** Read a dispatched replay's status (GET /api/v1/runner-tasks/:taskId). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> runStatus(String taskId) throws MockartyException {
        return client.get("/api/v1/runner-tasks/" + enc(taskId), Map.class);
    }

    /**
     * Poll {@link #runStatus} until the replay is terminal or the timeout
     * elapses. {@code intervalMs} defaults to 2000 when non-positive;
     * {@code timeoutMs} ≤ 0 means no timeout.
     */
    public Map<String, Object> waitForRun(String taskId, long intervalMs, long timeoutMs)
            throws MockartyException {
        long interval = intervalMs > 0 ? intervalMs : 2000L;
        long deadline = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : Long.MAX_VALUE;
        while (true) {
            Map<String, Object> st = runStatus(taskId);
            Object status = st.get("status");
            if (status != null && TERMINAL.contains(String.valueOf(status).toLowerCase())) {
                return st;
            }
            if (System.currentTimeMillis() >= deadline) {
                return st;
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return st;
            }
        }
    }

    /**
     * Export the recording as source. {@code lang} is one of "go", "python",
     * "java", "playwright", "appium".
     */
    public String export(String id, String lang) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/ui-tests/").append(enc(id))
                .append("/export?format=").append(enc(lang));
        String namespace = client.getConfig().getNamespace();
        if (namespace != null && !namespace.isEmpty()) {
            path.append("&namespace=").append(enc(namespace));
        }
        return new String(client.getBytes(path.toString()), StandardCharsets.UTF_8);
    }
}
