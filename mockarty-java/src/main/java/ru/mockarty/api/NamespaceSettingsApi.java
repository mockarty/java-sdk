// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CleanupPolicy;
import ru.mockarty.model.AutonomyNamespaceSettings;
import ru.mockarty.model.NamespaceUser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * API for namespace-level settings including users, cleanup, and webhooks.
 */
public class NamespaceSettingsApi {

    private final MockartyClient client;

    public NamespaceSettingsApi(MockartyClient client) {
        this.client = client;
    }

    /** Returns autonomous-mission defaults for the client's namespace. */
    public AutonomyNamespaceSettings getAutonomySettings() throws MockartyException {
        return client.get("/api/v1/autotester/settings", AutonomyNamespaceSettings.class);
    }

    /**
     * Replaces autonomous-mission defaults. Null run-window and retention values
     * are omitted and preserve current overrides; use
     * {@link #clearAutonomyRunWindow} or {@link #clearAutonomyRetention} for
     * explicit inheritance.
     */
    public AutonomyNamespaceSettings saveAutonomySettings(AutonomyNamespaceSettings settings) throws MockartyException {
        return saveAutonomySettings(settings, null);
    }

    /**
     * Retry-safe save variant. Reuse {@code requestId} after a timeout or lost
     * response so the server returns the original receipt without reapplying.
     */
    public AutonomyNamespaceSettings saveAutonomySettings(
            AutonomyNamespaceSettings settings, String requestId) throws MockartyException {
        if (settings == null) {
            throw new IllegalArgumentException("autonomy settings are required");
        }
        AutonomyNamespaceSettings current = getAutonomySettings();
        if (settings.getDefaultAutonomy() != null) {
            current.defaultAutonomy(settings.getDefaultAutonomy());
        }
        if (settings.getDefaultBudget() != null) {
            current.defaultBudget(settings.getDefaultBudget());
        }
        if (settings.getDefaultContextRefs() != null) {
            current.defaultContextRefs(settings.getDefaultContextRefs());
        }
        if (settings.getJournalEventRetentionDays() != null) {
            current.journalEventRetentionDays(settings.getJournalEventRetentionDays());
        }
        if (settings.getJournalPayloadRetentionDays() != null) {
            current.journalPayloadRetentionDays(settings.getJournalPayloadRetentionDays());
        }
        if (settings.getRunWindowMinutes() != null) {
            current.runWindowMinutes(settings.getRunWindowMinutes());
        }
        return putAutonomySettings(current, current.getEtag(), requestId);
    }

    /** Clears the namespace run-wall override so the effective lower layer applies. */
    @SuppressWarnings("unchecked")
    public AutonomyNamespaceSettings clearAutonomyRunWindow(String requestId) throws MockartyException {
        AutonomyNamespaceSettings current = getAutonomySettings();
        Map<String, Object> body = client.getObjectMapper().convertValue(current, Map.class);
        body.put("runWindowMinutes", null);
        return putAutonomySettings(body, current.getEtag(), requestId);
    }

    /**
     * Clears selected namespace retention overrides while preserving all other
     * autonomy settings and unselected retention values.
     */
    @SuppressWarnings("unchecked")
    public AutonomyNamespaceSettings clearAutonomyRetention(boolean clearEvent, boolean clearPayload)
            throws MockartyException {
        return clearAutonomyRetention(clearEvent, clearPayload, null);
    }

    /** Retry-safe clear variant using a caller-stable request identity. */
    @SuppressWarnings("unchecked")
    public AutonomyNamespaceSettings clearAutonomyRetention(
            boolean clearEvent, boolean clearPayload, String requestId) throws MockartyException {
        if (!clearEvent && !clearPayload) {
            throw new IllegalArgumentException("at least one retention override must be selected");
        }
        AutonomyNamespaceSettings current = getAutonomySettings();
        Map<String, Object> body = client.getObjectMapper().convertValue(current, Map.class);
        if (clearEvent) {
            body.put("journalEventRetentionDays", null);
        }
        if (clearPayload) {
            body.put("journalPayloadRetentionDays", null);
        }
        return putAutonomySettings(body, current.getEtag(), requestId);
    }

    private AutonomyNamespaceSettings putAutonomySettings(Object body, String etag, String requestId)
            throws MockartyException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Idempotency-Key", requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString() : requestId);
        if (etag != null && !etag.isBlank()) {
            headers.put("If-Match", etag);
        }
        return client.putWithHeaders("/api/v1/autotester/settings", body,
                AutonomyNamespaceSettings.class, headers);
    }

    // ---- Users ----

    /**
     * Lists users in a namespace.
     *
     * @param namespace the namespace name
     * @return list of namespace users
     */
    /**
     * Wire shape: server emits {@code {users: [...], total: N}} — NOT a
     * bare list. Older SDK builds tried to decode the envelope object
     * into {@code List<NamespaceUser>} and threw deserialization errors
     * on every call. The envelope wrapper is now unwrapped here.
     */
    @SuppressWarnings("unchecked")
    public List<NamespaceUser> listUsers(String namespace) throws MockartyException {
        Map<String, Object> env = client.get(
                "/api/v1/namespaces/" + encode(namespace) + "/users", Map.class);
        if (env == null) {
            return Collections.emptyList();
        }
        Object rawUsers = env.get("users");
        if (!(rawUsers instanceof List)) {
            return Collections.emptyList();
        }
        return client.getObjectMapper().convertValue(rawUsers,
                client.getObjectMapper().getTypeFactory()
                        .constructCollectionType(List.class, NamespaceUser.class));
    }

    /**
     * Adds a user to a namespace.
     *
     * @param namespace the namespace name
     * @param request   the add user request (userId, role)
     */
    public void addUser(String namespace, Map<String, Object> request) throws MockartyException {
        client.post("/api/v1/namespaces/" + encode(namespace) + "/users", request);
    }

    /**
     * Removes a user from a namespace.
     *
     * @param namespace the namespace name
     * @param userId    the user ID to remove
     */
    public void removeUser(String namespace, String userId) throws MockartyException {
        client.delete("/api/v1/namespaces/" + encode(namespace) + "/users/" + encode(userId));
    }

    /**
     * Updates a user's role in a namespace.
     *
     * @param namespace the namespace name
     * @param userId    the user ID
     * @param role      the new role
     */
    public void updateUserRole(String namespace, String userId, String role) throws MockartyException {
        client.put("/api/v1/namespaces/" + encode(namespace) + "/users/" + encode(userId) + "/role",
                Map.of("role", role));
    }

    // ---- Cleanup ----

    /**
     * Gets the cleanup policy for a namespace.
     *
     * @param namespace the namespace name
     * @return the cleanup policy
     */
    public CleanupPolicy getCleanupPolicy(String namespace) throws MockartyException {
        return client.get("/api/v1/namespaces/" + encode(namespace) + "/cleanup-policy", CleanupPolicy.class);
    }

    /**
     * Updates the cleanup policy for a namespace.
     *
     * @param namespace the namespace name
     * @param policy    the updated cleanup policy
     */
    public void updateCleanupPolicy(String namespace, CleanupPolicy policy) throws MockartyException {
        client.put("/api/v1/namespaces/" + encode(namespace) + "/cleanup-policy", policy);
    }

    // ---- Webhooks ----

    /**
     * Lists webhooks for a namespace.
     *
     * @param namespace the namespace name
     * @return list of webhook maps
     */
    /**
     * Wire shape: server emits {@code {webhooks: [...]}} — unwrap.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listWebhooks(String namespace) throws MockartyException {
        Map<String, Object> env = client.get(
                "/api/v1/namespaces/" + encode(namespace) + "/webhooks", Map.class);
        if (env == null) {
            return Collections.emptyList();
        }
        Object rawHooks = env.get("webhooks");
        if (!(rawHooks instanceof List)) {
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) rawHooks;
    }

    /**
     * Creates a webhook for a namespace.
     *
     * <p>Wire shape: server replies with {@code {message: ..., webhook: {...}}} —
     * unwrap the inner webhook before returning.
     *
     * @param namespace the namespace name
     * @param webhook   the webhook configuration
     * @return the created webhook
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createWebhook(String namespace, Map<String, Object> webhook) throws MockartyException {
        Map<String, Object> env = client.post(
                "/api/v1/namespaces/" + encode(namespace) + "/webhooks", webhook, Map.class);
        if (env == null) {
            return Collections.emptyMap();
        }
        Object inner = env.get("webhook");
        if (inner instanceof Map) {
            return (Map<String, Object>) inner;
        }
        return env;
    }

    /**
     * Deletes a webhook from a namespace.
     *
     * @param namespace the namespace name
     * @param id        the webhook ID to delete
     */
    public void deleteWebhook(String namespace, String id) throws MockartyException {
        client.delete("/api/v1/namespaces/" + encode(namespace) + "/webhooks/" + encode(id));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
