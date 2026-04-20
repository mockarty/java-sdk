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
 * Prompts Storage API — managed AI prompts with FIFO-20 version history
 * and rollback. Prompts are attached to AI buttons / TCM steps by ID so
 * editing a prompt propagates to every consumer.
 */
public class PromptsApi {

    private final MockartyClient client;

    public PromptsApi(MockartyClient client) {
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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list() throws MockartyException {
        String ns = client.getConfig().getNamespace();
        return client.get("/api/v1/stores/prompts?namespace=" + encode(ns), listOfMaps());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> create(String name, String body, Map<String, Object> extras) throws MockartyException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", name);
        payload.put("body", body);
        payload.put("namespace", client.getConfig().getNamespace());
        if (extras != null) payload.putAll(extras);
        return client.post("/api/v1/stores/prompts", payload, Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String promptId) throws MockartyException {
        return client.get("/api/v1/stores/prompts/" + encode(promptId), Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> update(String promptId, Map<String, Object> fields) throws MockartyException {
        return client.put("/api/v1/stores/prompts/" + encode(promptId), fields, Map.class);
    }

    public void delete(String promptId) throws MockartyException {
        client.delete("/api/v1/stores/prompts/" + encode(promptId));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listVersions(String promptId) throws MockartyException {
        return client.get("/api/v1/stores/prompts/" + encode(promptId) + "/versions", listOfMaps());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getVersion(String promptId, int version) throws MockartyException {
        return client.get(
                "/api/v1/stores/prompts/" + encode(promptId) + "/versions/" + version,
                Map.class);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> rollback(String promptId, int toVersion) throws MockartyException {
        return client.post(
                "/api/v1/stores/prompts/" + encode(promptId) + "/rollback?to=" + toVersion,
                null, Map.class);
    }
}
