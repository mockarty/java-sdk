// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Mock;
import ru.mockarty.model.Page;
import ru.mockarty.model.SaveMockResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * API for CRUD operations on mocks.
 */
public class MockApi {

    private final MockartyClient client;

    public MockApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Creates a new mock or overwrites an existing one with the same ID.
     *
     * @param mock the mock to create
     * @return the save response indicating whether it was an overwrite
     */
    public SaveMockResponse create(Mock mock) throws MockartyException {
        return client.post("/api/v1/mocks", mock, SaveMockResponse.class);
    }

    /**
     * Retrieves a mock by its ID.
     *
     * @param id the mock ID
     * @return the mock
     */
    public Mock get(String id) throws MockartyException {
        return client.get("/api/v1/mocks/" + encode(id), Mock.class);
    }

    /**
     * Lists mocks with default pagination.
     *
     * @return a page of mocks
     */
    public Page<Mock> list() throws MockartyException {
        return list(null, null, null, 0, 50);
    }

    /**
     * Lists mocks with filtering and pagination.
     *
     * @param namespace filter by namespace (null for default)
     * @param tags      filter by tags (null for no filter)
     * @param search    search text (null for no filter)
     * @param offset    pagination offset
     * @param limit     pagination limit
     * @return a page of mocks
     */
    public Page<Mock> list(String namespace, List<String> tags, String search, int offset, int limit) throws MockartyException {
        StringJoiner query = new StringJoiner("&", "?", "");
        query.add("offset=" + offset);
        query.add("limit=" + limit);

        if (namespace != null && !namespace.isEmpty()) {
            query.add("namespace=" + encode(namespace));
        } else {
            query.add("namespace=" + encode(client.getConfig().getNamespace()));
        }

        if (tags != null && !tags.isEmpty()) {
            for (String tag : tags) {
                query.add("tags=" + encode(tag));
            }
        }

        if (search != null && !search.isEmpty()) {
            query.add("search=" + encode(search));
        }

        JavaType pageType = client.getObjectMapper().getTypeFactory()
                .constructParametricType(Page.class, Mock.class);
        return client.get("/api/v1/mocks" + query.toString(), pageType);
    }

    /**
     * Deletes a mock by its ID (soft delete).
     *
     * @param id the mock ID
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/mocks/" + encode(id));
    }

    /**
     * Restores a soft-deleted mock by its ID.
     *
     * @param id the mock ID
     */
    public Mock restore(String id) throws MockartyException {
        return client.post("/api/v1/mocks/" + encode(id) + "/restore", null, Mock.class);
    }

    /**
     * Permanently purges a mock by its ID.
     *
     * @param id the mock ID
     */
    public void purge(String id) throws MockartyException {
        client.delete("/api/v1/mocks/" + encode(id) + "/purge");
    }

    /**
     * Gets all mocks in a chain by chain ID.
     *
     * @param chainId the chain ID
     * @return list of mocks in the chain
     */
    @SuppressWarnings("unchecked")
    public List<Mock> getChain(String chainId) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Mock.class);
        return client.get("/api/v1/mocks/chains/" + encode(chainId), listType);
    }

    /**
     * Deletes all mocks in a chain by chain ID.
     *
     * @param chainId the chain ID
     */
    public void deleteChain(String chainId) throws MockartyException {
        client.delete("/api/v1/mocks/chains/" + encode(chainId));
    }

    /**
     * Copies mocks to another namespace.
     *
     * @param mockIds         list of mock IDs to copy
     * @param targetNamespace the target namespace
     */
    public void copyToNamespace(List<String> mockIds, String targetNamespace) throws MockartyException {
        Map<String, Object> body = Map.of(
                "ids", mockIds,
                "targetNamespace", targetNamespace
        );
        client.post("/api/v1/mocks/copy-to-namespace", body);
    }

    /**
     * Gets request logs for a mock by its ID.
     *
     * @param id the mock ID
     * @return the logs as a list of maps
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> logs(String id) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Map.class);
        return client.get("/api/v1/mocks/" + encode(id) + "/logs", listType);
    }

    /**
     * Lists all versions of a mock.
     *
     * @param id the mock ID
     * @return list of mock versions
     */
    public List<Mock> listVersions(String id) throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Mock.class);
        return client.get("/api/v1/mocks/" + encode(id) + "/versions", listType);
    }

    /**
     * Gets a specific version of a mock.
     *
     * @param id      the mock ID
     * @param version the version identifier
     * @return the mock at that version
     */
    public Mock getVersion(String id, String version) throws MockartyException {
        return client.get("/api/v1/mocks/" + encode(id) + "/versions/" + encode(version), Mock.class);
    }

    /**
     * Restores a specific version of a mock.
     *
     * @param id      the mock ID
     * @param version the version identifier to restore
     */
    public void restoreVersion(String id, String version) throws MockartyException {
        client.post("/api/v1/mocks/" + encode(id) + "/versions/" + encode(version) + "/restore", null);
    }

    /**
     * Partially updates a mock using a patch object.
     *
     * @param id    the mock ID
     * @param patch the fields to update
     * @return the updated mock
     */
    public Mock patchMock(String id, Map<String, Object> patch) throws MockartyException {
        return client.patch("/api/v1/mocks/" + encode(id), patch, Mock.class);
    }

    /**
     * Deletes all request logs for a mock.
     *
     * @param id the mock ID
     */
    public void deleteLogs(String id) throws MockartyException {
        client.delete("/api/v1/mocks/" + encode(id) + "/logs");
    }

    /**
     * Moves mocks to a folder.
     *
     * @param mockIds  list of mock IDs to move
     * @param folderId the target folder ID
     */
    public void moveToFolder(List<String> mockIds, String folderId) throws MockartyException {
        Map<String, Object> body = Map.of(
                "ids", mockIds,
                "folderId", folderId
        );
        client.patch("/api/v1/mocks/batch/move", body);
    }

    /**
     * Batch updates tags for multiple mocks.
     *
     * @param mockIds list of mock IDs
     * @param tags    the tags to set
     */
    public void batchUpdateTags(List<String> mockIds, List<String> tags) throws MockartyException {
        Map<String, Object> body = Map.of(
                "ids", mockIds,
                "tags", tags
        );
        client.patch("/api/v1/mocks/batch/tags", body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
