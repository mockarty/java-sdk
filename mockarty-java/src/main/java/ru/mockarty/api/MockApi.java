// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Mock;
import ru.mockarty.model.MockVersion;
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
     * Creates a mock, resolving a duplicate-entity conflict via {@code intent}.
     *
     * <p>When a similar mock already exists the server returns HTTP 409
     * {@code duplicate_entity}. Pass {@code "create_new"} to keep both (e.g.
     * several condition-differentiated mocks on one route) or {@code "overwrite"}
     * to replace the existing one in place. A null/blank intent behaves like
     * {@link #create(Mock)}.
     *
     * @param mock   the mock to create
     * @param intent {@code "create_new"} or {@code "overwrite"} (or null)
     * @return the save response
     */
    public SaveMockResponse create(Mock mock, String intent) throws MockartyException {
        String path = "/api/v1/mocks";
        if (intent != null && !intent.isEmpty()) {
            path += "?intent=" + encode(intent);
        }
        return client.post(path, mock, SaveMockResponse.class);
    }

    /**
     * Creates or overwrites the mock with the given ID — parity with the Go SDK
     * ({@code MockAPI.Update(id, mock)}) and Python ({@code mocks.update(id, mock)}),
     * which the Java SDK was missing (3-language parity gap found via cross-SDK
     * audit). Mockarty updates a mock by POSTing it with the same ID, so this
     * stamps the ID onto the payload and saves.
     *
     * @param id   the mock ID to update
     * @param mock the new mock state
     * @return the saved mock
     */
    public Mock update(String id, Mock mock) throws MockartyException {
        if (id != null && !id.isEmpty()) {
            mock.id(id);
        }
        SaveMockResponse resp = client.post("/api/v1/mocks", mock, SaveMockResponse.class);
        return resp.getMock();
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
        return list(namespace, tags, search, null, null, null, offset, limit);
    }

    /**
     * Lists mocks with every server-side catalogue filter.
     *
     * @param namespace  filter by namespace (null for default)
     * @param tags       filter by tags (null for no filter)
     * @param search     search text (null for no filter)
     * @param folderId   folder UUID or {@code root} (null for no filter)
     * @param protocol   protocol name (null for no filter)
     * @param onlyActive whether to exclude soft-deleted mocks (null for server default)
     * @param offset     pagination offset
     * @param limit      pagination limit
     * @return a page of mocks
     */
    public Page<Mock> list(String namespace, List<String> tags, String search,
                           String folderId, String protocol, Boolean onlyActive,
                           int offset, int limit) throws MockartyException {
        StringJoiner query = new StringJoiner("&", "?", "");
        query.add("offset=" + offset);
        query.add("limit=" + limit);

        if (namespace != null && !namespace.isEmpty()) {
            query.add("namespace=" + encode(namespace));
        } else {
            query.add("namespace=" + encode(client.getConfig().getNamespace()));
        }

        if (tags != null && !tags.isEmpty()) {
            query.add("tags=" + encode(String.join(",", tags)));
        }

        if (search != null && !search.isEmpty()) {
            query.add("search=" + encode(search));
        }

        if (folderId != null && !folderId.isEmpty()) {
            query.add("folderId=" + encode(folderId));
        }

        if (protocol != null && !protocol.isEmpty()) {
            query.add("protocol=" + encode(protocol));
        }

        if (onlyActive != null) {
            query.add("onlyActive=" + onlyActive);
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
     * <p>Server contract: there is no per-ID restore endpoint; the
     * single-row case routes through the batch endpoint with a
     * one-element {@code mockIds} list. Older SDK builds POSTed to
     * {@code /api/v1/mocks/:id/restore} (which doesn't exist) and
     * every call 404'd.
     *
     * @param id the mock ID
     */
    public Mock restore(String id) throws MockartyException {
        client.post("/api/v1/mocks/batch/restore",
                Map.of("mockIds", java.util.List.of(id)));
        // Server's batch/restore returns {message, restored:N} with no
        // mock body; pull the fresh row via Get so the public signature
        // (Mock return) stays stable.
        return get(id);
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
        // Server wire field is mockIds, NOT ids — older SDK builds 400'd
        // every call with 'invalid request payload'.
        Map<String, Object> body = Map.of(
                "mockIds", mockIds,
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
        // Server returns model.LogsMock -> {"id", "requests":[...]} (an
        // object), NOT a bare array. Deserializing as a List threw
        // "Cannot deserialize value of type List from Object value" on every
        // call; read the envelope and pull the rows out of "requests".
        JavaType mapType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class, String.class, Object.class);
        Map<String, Object> envelope =
                client.get("/api/v1/mocks/" + encode(id) + "/logs", mapType);
        if (envelope != null && envelope.get("requests") instanceof List) {
            return (List<Map<String, Object>>) envelope.get("requests");
        }
        return List.of();
    }

    /**
     * Lists a mock's version history, newest first.
     *
     * <p>Wire shape: {@code {mock_id, versions: [...], count}}. The rows are
     * revision records, not mocks — the mock body of a revision hangs off
     * {@link MockVersion#getMock()}. (Decoding the envelope as a bare
     * {@code List<Mock>} yielded an empty list for every mock that had a
     * history.)</p>
     *
     * @param id the mock ID
     * @return the revision rows
     */
    public List<MockVersion> listVersions(String id) throws MockartyException {
        JavaType envelopeType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class,
                        client.getObjectMapper().getTypeFactory().constructType(String.class),
                        client.getObjectMapper().getTypeFactory().constructType(Object.class));
        Map<String, Object> envelope =
                client.get("/api/v1/mocks/" + encode(id) + "/versions", envelopeType);
        Object rows = envelope == null ? null : envelope.get("versions");
        if (!(rows instanceof List)) {
            return List.of();
        }
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, MockVersion.class);
        return client.getObjectMapper().convertValue(rows, listType);
    }

    /**
     * Gets a specific revision of a mock.
     *
     * <p>Wire shape: {@code {version: {...}, previous_version: {...}}} — the
     * envelope is unwrapped here. Use
     * {@link #getVersionWithPrevious(String, String)} when the preceding
     * revision is needed for a diff.</p>
     *
     * @param id      the mock ID
     * @param version the revision number
     * @return the revision row
     */
    public MockVersion getVersion(String id, String version) throws MockartyException {
        return getVersionWithPrevious(id, version)[0];
    }

    /**
     * Gets a revision together with the one before it.
     *
     * @return a two-element array: {@code [current, previous]}; {@code previous}
     *         is {@code null} for the first revision
     * @throws MockartyException when the revision does not exist — returning an
     *         empty row would read as "revision 0 exists"
     */
    public MockVersion[] getVersionWithPrevious(String id, String version)
            throws MockartyException {
        JavaType envelopeType = client.getObjectMapper().getTypeFactory()
                .constructMapType(Map.class,
                        client.getObjectMapper().getTypeFactory().constructType(String.class),
                        client.getObjectMapper().getTypeFactory().constructType(Object.class));
        Map<String, Object> envelope = client.get(
                "/api/v1/mocks/" + encode(id) + "/versions/" + encode(version), envelopeType);
        Object current = envelope == null ? null : envelope.get("version");
        if (current == null) {
            throw new MockartyException(
                    "mockarty: mock " + id + " has no version " + version);
        }
        Object previous = envelope.get("previous_version");
        return new MockVersion[] {
                client.getObjectMapper().convertValue(current, MockVersion.class),
                previous == null
                        ? null
                        : client.getObjectMapper().convertValue(previous, MockVersion.class),
        };
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
    public Mock patch(String id, Map<String, Object> patch) throws MockartyException {
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
     * Batch creates multiple mocks in one request.
     *
     * @param mocks list of mocks to create
     */
    public void batchCreate(List<Mock> mocks) throws MockartyException {
        client.post("/api/v1/mocks/batch", Map.of("mocks", mocks));
    }

    /**
     * Batch deletes multiple mocks by their IDs (soft delete).
     *
     * @param ids list of mock IDs to delete
     */
    public void batchDelete(List<String> ids) throws MockartyException {
        // Server reads mockIds, not ids — see CopyToNamespace note.
        client.delete("/api/v1/mocks/batch", Map.of("mockIds", ids));
    }

    /**
     * Batch restores multiple soft-deleted mocks.
     *
     * @param ids list of mock IDs to restore
     */
    public void batchRestore(List<String> ids) throws MockartyException {
        client.post("/api/v1/mocks/batch/restore", Map.of("mockIds", ids));
    }

    /**
     * Moves mocks to a folder.
     *
     * <p>Server reads {@code mockIds} + {@code folderId}.
     *
     * @param mockIds  list of mock IDs to move
     * @param folderId the target folder ID
     */
    public void moveToFolder(List<String> mockIds, String folderId) throws MockartyException {
        Map<String, Object> body = Map.of(
                "mockIds", mockIds,
                "folderId", folderId
        );
        client.patch("/api/v1/mocks/batch/move", body);
    }

    /**
     * Batch updates tags for multiple mocks. The single {@code tags}
     * argument is treated as "tags to add" since the server splits
     * add and remove deltas (see {@code tagsToAdd} / {@code tagsToRemove}).
     *
     * @param mockIds list of mock IDs
     * @param tags    the tags to add to every mock in mockIds
     */
    public void batchUpdateTags(List<String> mockIds, List<String> tags) throws MockartyException {
        Map<String, Object> body = Map.of(
                "mockIds", mockIds,
                "tagsToAdd", tags
        );
        client.patch("/api/v1/mocks/batch/tags", body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
