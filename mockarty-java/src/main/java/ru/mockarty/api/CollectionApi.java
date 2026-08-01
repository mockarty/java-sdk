// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

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
    public Map<String, Object> execute(String id) throws MockartyException {
        return client.post("/api/v1/api-tester/collections/" + encode(id) + "/execute", null, Map.class);
    }

    /**
     * Deletes a test collection.
     *
     * @param id the collection ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/api-tester/collections/" + encode(id));
    }

    /** Updates a collection by ID. Parity: Go Update / Python update. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> update(String id, Map<String, Object> collection) throws MockartyException {
        return client.put("/api/v1/api-tester/collections/" + encode(id), collection, Map.class);
    }

    /** Duplicates a collection by ID. Parity: Go Duplicate / Python duplicate. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> duplicate(String id) throws MockartyException {
        return client.post("/api/v1/api-tester/collections/" + encode(id) + "/duplicate", null, Map.class);
    }

    /** Deletes multiple collections by ID. Parity: Go BatchDelete / Python batch_delete. */
    public void batchDelete(java.util.List<String> ids) throws MockartyException {
        client.delete("/api/v1/api-tester/collections/batch", Map.of("ids", ids));
    }

    /** Executes tests from multiple collections. Parity: Go ExecuteMultiple / Python execute_multiple. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> executeMultiple(java.util.List<String> ids) throws MockartyException {
        return client.post("/api/v1/api-tester/collections/execute-multiple",
                Map.of("collectionIds", ids), Map.class);
    }

    /** Exports a collection as bytes. Parity: Go Export / Python export. */
    public byte[] export(String id) throws MockartyException {
        return client.getBytes("/api/v1/api-tester/collections/" + encode(id) + "/export");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
