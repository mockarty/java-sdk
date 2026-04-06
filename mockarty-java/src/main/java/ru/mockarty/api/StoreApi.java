// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * API for Global and Chain store operations.
 *
 * <p>Stores in Mockarty:</p>
 * <ul>
 *   <li><b>Global Store (gS)</b> - Namespace-scoped global state</li>
 *   <li><b>Chain Store (cS)</b> - Request chain state across related mocks</li>
 *   <li><b>Mock Store (mS)</b> - Per-mock-call state (managed automatically)</li>
 * </ul>
 */
public class StoreApi {

    private final MockartyClient client;

    public StoreApi(MockartyClient client) {
        this.client = client;
    }

    // Global Store operations

    /**
     * Gets the entire global store for the current namespace.
     *
     * @return the global store as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> globalGet() throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        return client.get("/api/v1/stores/global?namespace=" + encode(namespace), Map.class);
    }

    /**
     * Gets the entire global store for a specific namespace.
     *
     * @param namespace the namespace
     * @return the global store as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> globalGet(String namespace) throws MockartyException {
        return client.get("/api/v1/stores/global?namespace=" + encode(namespace), Map.class);
    }

    /**
     * Sets a key-value pair in the global store.
     *
     * @param key   the key
     * @param value the value
     */
    public void globalSet(String key, Object value) throws MockartyException {
        Map<String, Object> body = Map.of(
                "namespace", client.getConfig().getNamespace(),
                "key", key,
                "value", value
        );
        client.post("/api/v1/stores/global", body);
    }

    /**
     * Sets a key-value pair in the global store for a specific namespace.
     *
     * @param namespace the namespace
     * @param key       the key
     * @param value     the value
     */
    public void globalSet(String namespace, String key, Object value) throws MockartyException {
        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "key", key,
                "value", value
        );
        client.post("/api/v1/stores/global", body);
    }

    /**
     * Deletes a key from the global store.
     *
     * @param key the key to delete
     */
    public void globalDelete(String key) throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        client.delete("/api/v1/stores/global/" + encode(key) + "?namespace=" + encode(namespace));
    }

    // Chain Store operations

    /**
     * Gets the chain store for a specific chain ID.
     *
     * @param chainId the chain ID
     * @return the chain store as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chainGet(String chainId) throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        return client.get("/api/v1/stores/chain/" + encode(chainId) + "?namespace=" + encode(namespace), Map.class);
    }

    /**
     * Gets the chain store for a specific chain ID and namespace.
     *
     * @param chainId   the chain ID
     * @param namespace the namespace
     * @return the chain store as a map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> chainGet(String chainId, String namespace) throws MockartyException {
        return client.get("/api/v1/stores/chain/" + encode(chainId) + "?namespace=" + encode(namespace), Map.class);
    }

    /**
     * Sets a key-value pair in a chain store.
     *
     * @param chainId the chain ID
     * @param key     the key
     * @param value   the value
     */
    public void chainSet(String chainId, String key, Object value) throws MockartyException {
        Map<String, Object> body = Map.of(
                "namespace", client.getConfig().getNamespace(),
                "key", key,
                "value", value
        );
        client.post("/api/v1/stores/chain/" + encode(chainId), body);
    }

    /**
     * Sets a key-value pair in a chain store for a specific namespace.
     *
     * @param chainId   the chain ID
     * @param namespace the namespace
     * @param key       the key
     * @param value     the value
     */
    public void chainSet(String chainId, String namespace, String key, Object value) throws MockartyException {
        Map<String, Object> body = Map.of(
                "namespace", namespace,
                "key", key,
                "value", value
        );
        client.post("/api/v1/stores/chain/" + encode(chainId), body);
    }

    /**
     * Deletes a key from a chain store.
     *
     * @param chainId the chain ID
     * @param key     the key to delete
     */
    public void chainDelete(String chainId, String key) throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        client.delete("/api/v1/stores/chain/" + encode(chainId) + "/" + encode(key) + "?namespace=" + encode(namespace));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
