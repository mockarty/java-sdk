// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Secrets Storage API — centralised encrypted key/value stores
 * (Phase A0). Decrypted values are only returned from
 * {@link #getEntry(String, String)} and only to callers whose API key
 * carries the {@code secret:read} permission.
 *
 * <p>Server wire shapes (envelope-wrapped responses):
 * <pre>
 *   GET    /stores/secrets             → {"stores": [...], "namespace": "..."}
 *   POST   /stores/secrets             → {"store":  {...}}
 *   GET    /stores/secrets/:id         → {"store":  {...}}
 *   PUT    /stores/secrets/:id         → {"store":  {...}}
 *   GET    /stores/secrets/:id/entries → {"entries": [...], ...}
 *   POST   /stores/secrets/:id/entries → {"entry":   <flat dict>}
 *   GET    /stores/secrets/:id/entries/:k → <flat dict>   (NO envelope)
 *   PUT    /stores/secrets/:id/entries/:k → {"message": "...", "id": "..."}
 *   POST   /stores/secrets/:id/entries/:k/rotate → {"message": "..."}
 * </pre>
 *
 * <p>This client unwraps every {@code store}/{@code stores}/{@code entry}/
 * {@code entries} envelope before returning so callers see the inner
 * object directly. The namespace is threaded onto the query string for
 * every call (the server reads NS from {@code ?namespace=} only —
 * passing it in the request body is silently ignored).
 *
 * <p>Valid backend values: {@code inline} (default, local AES-GCM via
 * the admin node's KeyStore), {@code vault}, {@code aws_sm},
 * {@code gcp_sm}, {@code azure_kv}, {@code custom_api}.
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

    /** Returns {@code env[key]} when present and a Map, else the envelope itself. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapOne(Map<String, Object> env, String key) {
        if (env == null) {
            return Collections.emptyMap();
        }
        Object inner = env.get(key);
        if (inner instanceof Map) {
            return (Map<String, Object>) inner;
        }
        return env;
    }

    /** Returns {@code env[key]} when present and a List, else the empty list. */
    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> unwrapList(Map<String, Object> env, String key) {
        if (env == null) {
            return Collections.emptyList();
        }
        Object inner = env.get(key);
        if (inner instanceof List) {
            return (List<Map<String, Object>>) inner;
        }
        return Collections.emptyList();
    }

    private String nsQuery() {
        return "?namespace=" + encode(client.getConfig().getNamespace());
    }

    private String nsQuery(String namespace) {
        String ns = (namespace == null || namespace.isEmpty())
                ? client.getConfig().getNamespace() : namespace;
        return "?namespace=" + encode(ns);
    }

    // ── Stores ──

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listStores() throws MockartyException {
        // Envelope shape: {"stores": [...], "namespace": "..."}.
        Map<String, Object> env = client.get(
                "/api/v1/stores/secrets" + nsQuery(),
                Map.class);
        return unwrapList(env, "stores");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createStore(String name, String description, String backend) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        if (description != null) body.put("description", description);
        // Default to the only widely-available backend the server actually
        // ships — the previous "software" string was rejected as
        // "unsupported backend" since the server enum is
        // inline|vault|aws_sm|gcp_sm|azure_kv|custom_api.
        body.put("backend", backend == null ? "inline" : backend);
        Map<String, Object> env = client.post(
                "/api/v1/stores/secrets" + nsQuery(),
                body, Map.class);
        return unwrapOne(env, "store");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getStore(String storeId) throws MockartyException {
        Map<String, Object> env = client.get(
                "/api/v1/stores/secrets/" + encode(storeId) + nsQuery(),
                Map.class);
        return unwrapOne(env, "store");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateStore(String storeId, Map<String, Object> fields) throws MockartyException {
        Map<String, Object> env = client.put(
                "/api/v1/stores/secrets/" + encode(storeId) + nsQuery(),
                fields, Map.class);
        return unwrapOne(env, "store");
    }

    public void deleteStore(String storeId) throws MockartyException {
        client.delete("/api/v1/stores/secrets/" + encode(storeId) + nsQuery());
    }

    // ── Entries ──

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listEntries(String storeId) throws MockartyException {
        // Envelope shape: {"entries": [...], "store_id": "...", "namespace": "..."}.
        Map<String, Object> env = client.get(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries" + nsQuery(),
                Map.class);
        return unwrapList(env, "entries");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> createEntry(String storeId, String key, String value, String description) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("key", key);
        body.put("value", value);
        if (description != null) body.put("description", description);
        Map<String, Object> env = client.post(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries" + nsQuery(),
                body, Map.class);
        return unwrapOne(env, "entry");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntry(String storeId, String key) throws MockartyException {
        // GET on a single entry is NOT enveloped — the server returns a flat
        // dict {id, key, value, version, sensitive, ...} so we read it
        // directly.
        return client.get(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key) + nsQuery(),
                Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> updateEntry(String storeId, String key, String value, String description) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("value", value);
        if (description != null) body.put("description", description);
        // PUT returns {"message": "entry updated", "id": "..."} — no
        // envelope to unwrap; return as-is.
        return client.put(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key) + nsQuery(),
                body, Map.class);
    }

    /**
     * Replace the entry's value with {@code newValue}, bumping its
     * version. Server requires {@code {"value": ...}} in the body —
     * older SDK builds posted {@code null} and rotate calls 400'd with
     * 'invalid request payload'. The new value is mandatory.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> rotateEntry(String storeId, String key, String newValue) throws MockartyException {
        if (newValue == null || newValue.isEmpty()) {
            throw new MockartyException("rotateEntry: newValue is required");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("value", newValue);
        return client.post(
                "/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key) + "/rotate" + nsQuery(),
                body, Map.class);
    }

    public void deleteEntry(String storeId, String key) throws MockartyException {
        client.delete("/api/v1/stores/secrets/" + encode(storeId) + "/entries/" + encode(key) + nsQuery());
    }

    // ── Vault integration ──

    public void configureVault(String namespace, Map<String, Object> config) throws MockartyException {
        String ns = namespace == null ? client.getConfig().getNamespace() : namespace;
        client.put("/api/v1/namespaces/" + encode(ns) + "/integrations/vault", config);
    }
}
