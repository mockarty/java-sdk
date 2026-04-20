// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secrets Storage API — centralised encrypted key/value stores
 * (Phase A0). Decrypted values are only returned from
 * {@link #getEntry(String, String)} and only to callers whose API key
 * carries the {@code secret:read} permission.
 */
public class SecretsApi {

    private final MockartyClient client;

    public SecretsApi(MockartyClient client) {
        this.client = client;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private JavaType listOfMaps() {
        TypeFactory tf = client.getObjectMapper().getTypeFactory();
        return tf.constructCollectionType(List.class,
                tf.constructMapType(Map.class, String.class, Object.class));
    }

    // ── Stores ──

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listStores() throws MockartyException {
        String ns = client.getConfig().getNamespace();
        return client.get("/api/v1/stores/secrets?namespace=" + encode(ns), listOfMaps());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createStore(String name, String description, String backend) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("namespace", client.getConfig().getNamespace());
        if (description != null) body.put("description", description);
        body.put("backend", backend == null ? "software" : backend);
        return client.post("/api/v1/stores/secrets", body, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getStore(String storeId) throws MockartyException {
        return client.get("/api/v1/stores/secrets/" + encode(storeId), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateStore(String storeId, Map<String, Object> fields) throws MockartyException {
        return client.put("/api/v1/stores/secrets/" + encode(storeId), fields, Map.class);
    }

    public void deleteStore(String storeId) throws MockartyException {
        client.delete("/api/v1/stores/secrets/" + encode(storeId));
    }

    // ── Entries ──

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listEntries(String storeId) throws MockartyException {
        return client.get("/api/v1/stores/secrets/" + encode(storeId) + "/entries", listOfMaps());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createEntry(String storeId, String key, String value, String description) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("value", value);
        if (description != null) body.put("description", description);
        return client.post("/api/v1/stores/secrets/" + encode(storeId) + "/entries", body, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntry(String storeId, String key) throws MockartyException {
        return client.get(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateEntry(String storeId, String key, String value, String description) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("value", value);
        if (description != null) body.put("description", description);
        return client.put(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key),
                body, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> rotateEntry(String storeId, String key) throws MockartyException {
        return client.post(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key) + "/rotate",
                null, Map.class);
    }

    public void deleteEntry(String storeId, String key) throws MockartyException {
        client.delete("/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key));
    }

    // ── Vault integration ──

    public void configureVault(String namespace, Map<String, Object> config) throws MockartyException {
        String ns = namespace == null ? client.getConfig().getNamespace() : namespace;
        client.put("/api/v1/namespaces/" + encode(ns) + "/integrations/vault", config);
    }
}
