// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for test collection management.
 * Collections organize API test requests for execution and scheduling.
 */
public class CollectionApi {

    private final MockartyClient client;

    public CollectionApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all test collections for the current namespace.
     *
     * @return list of collection metadata
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> list() throws MockartyException {
        String namespace = client.getConfig().getNamespace();
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        return client.get("/api/v1/api-tester/collections?namespace=" + encode(namespace), listType);
    }

    /**
     * Gets a specific collection by ID.
     *
     * @param id the collection ID
     * @return the collection data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String id) throws MockartyException {
        return client.get("/api/v1/api-tester/collections/" + encode(id), Map.class);
    }

    /**
     * Creates a new test collection.
     *
     * @param collection the collection data
     * @return the created collection
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(Map<String, Object> collection) throws MockartyException {
        return client.post("/api/v1/api-tester/collections", collection, Map.class);
    }

    /**
     * Runs a test collection.
     *
     * @param id the collection ID to run
     * @return the run results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(String id) throws MockartyException {
        return client.post("/api/v1/api-tester/collections/" + encode(id) + "/run", null, Map.class);
    }

    /**
     * Deletes a test collection.
     *
     * @param id the collection ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/api-tester/collections/" + encode(id));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
