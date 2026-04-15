// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.CleanupPolicy;
import ru.mockarty.model.NamespaceUser;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for namespace-level settings including users, cleanup, and webhooks.
 */
public class NamespaceSettingsApi {

    private final MockartyClient client;

    public NamespaceSettingsApi(MockartyClient client) {
        this.client = client;
    }

    // ---- Users ----

    /**
     * Lists users in a namespace.
     *
     * @param namespace the namespace name
     * @return list of namespace users
     */
    public List<NamespaceUser> listUsers(String namespace) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, NamespaceUser.class);
        return client.get("/api/v1/namespaces/" + encode(namespace) + "/users", listType);
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
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listWebhooks(String namespace) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        return client.get("/api/v1/namespaces/" + encode(namespace) + "/webhooks", listType);
    }

    /**
     * Creates a webhook for a namespace.
     *
     * @param namespace the namespace name
     * @param webhook   the webhook configuration
     * @return the created webhook
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createWebhook(String namespace, Map<String, Object> webhook) throws MockartyException {
        return client.post("/api/v1/namespaces/" + encode(namespace) + "/webhooks",
                webhook, Map.class);
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
