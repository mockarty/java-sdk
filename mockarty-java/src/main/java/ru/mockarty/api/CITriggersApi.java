// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for generic-webhook CI Triggers.
 *
 * <p>Exposes ONLY the surface useful from a CI/CD script's perspective
 * (per Mockarty SDK scope policy): list saved triggers, get one,
 * read the linked run status, cancel a run. CRUD of trigger
 * configurations is the administrative UI's concern and is intentionally
 * NOT in this SDK.
 *
 * <p>Distinct from {@code CITriggerApi} (singular) — that is the older
 * Allure TCM-bound provider; see {@code internal/ci/README.md} in the
 * server repo for the split.
 */
public class CITriggersApi {

    private final MockartyClient client;

    public CITriggersApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists saved CI triggers in a namespace.
     *
     * @param namespace tenant namespace; null/empty falls back to the
     *                  caller's pinned namespace.
     * @return list of trigger summaries (id, name, triggerUrl,
     *         templateKind, enabled). Auth secret is omitted (server
     *         returns {@code "***"} regardless).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list(String namespace) throws MockartyException {
        String path = "/api/v1/ci/triggers";
        if (namespace != null && !namespace.isEmpty()) {
            path += "?namespace=" + encode(namespace);
        }
        Map<String, Object> resp = client.get(path, Map.class);
        if (resp == null) return java.util.Collections.emptyList();
        Object triggers = resp.get("triggers");
        if (triggers instanceof List) {
            return (List<Map<String, Object>>) triggers;
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Fetches a single trigger. Cross-namespace lookups return 404
     * (existence is hidden) — equivalent to "unknown ID" from the
     * caller's perspective by design.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String triggerId, String namespace) throws MockartyException {
        String path = "/api/v1/ci/triggers/" + encode(triggerId);
        if (namespace != null && !namespace.isEmpty()) {
            path += "?namespace=" + encode(namespace);
        }
        Map<String, Object> resp = client.get(path, Map.class);
        if (resp == null) return null;
        Object trigger = resp.get("trigger");
        return trigger instanceof Map ? (Map<String, Object>) trigger : null;
    }

    /**
     * Reads the CI run linked to a Mockarty task. Returns null when no
     * CI run is associated with the task (e.g. the launch did not set
     * {@code ciTriggerId}).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getRunByTask(String taskId) throws MockartyException {
        try {
            Map<String, Object> resp = client.get(
                "/api/v1/ci/runs?taskId=" + encode(taskId), Map.class);
            if (resp == null) return null;
            Object run = resp.get("run");
            return run instanceof Map ? (Map<String, Object>) run : null;
        } catch (MockartyException e) {
            // 404 is the documented "no CI run for this task" response —
            // surface as null, not as an exception, so the calling
            // poll loop is simpler.
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Marks a CI run as cancelled and fails the linked Mockarty task.
     * Best-effort — does NOT call any remote CI cancel API.
     */
    public void cancelRun(String runId) throws MockartyException {
        client.post("/api/v1/ci/runs/" + encode(runId) + "/cancel", null);
    }

    /** Creates a CI trigger. Parity: Go Create / Python create. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String namespace, Map<String, Object> trigger) throws MockartyException {
        String path = "/api/v1/ci/triggers";
        if (namespace != null && !namespace.isEmpty()) path += "?namespace=" + encode(namespace);
        return client.post(path, trigger, Map.class);
    }

    /** Updates a CI trigger. Parity: Go Update / Python update. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> update(String triggerId, String namespace, Map<String, Object> trigger) throws MockartyException {
        String path = "/api/v1/ci/triggers/" + encode(triggerId);
        if (namespace != null && !namespace.isEmpty()) path += "?namespace=" + encode(namespace);
        return client.patch(path, trigger, Map.class);
    }

    /** Deletes a CI trigger. Parity: Go Delete / Python delete. */
    public void delete(String triggerId, String namespace) throws MockartyException {
        String path = "/api/v1/ci/triggers/" + encode(triggerId);
        if (namespace != null && !namespace.isEmpty()) path += "?namespace=" + encode(namespace);
        client.delete(path);
    }

    /** Test-fires a CI trigger's dispatch. Parity: Go TestDispatch / Python test_dispatch. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> testDispatch(String triggerId, String namespace) throws MockartyException {
        String path = "/api/v1/ci/triggers/" + encode(triggerId) + "/test";
        if (namespace != null && !namespace.isEmpty()) path += "?namespace=" + encode(namespace);
        return client.post(path, java.util.Collections.emptyMap(), Map.class);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
