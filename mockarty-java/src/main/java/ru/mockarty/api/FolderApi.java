// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.MockFolder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for mock folder management.
 */
public class FolderApi {

    private final MockartyClient client;

    public FolderApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all mock folders.
     *
     * @return list of folders
     */
    public List<MockFolder> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, MockFolder.class);
        return client.get("/api/v1/mock-folders", listType);
    }

    /**
     * Creates a new mock folder.
     *
     * @param folder the folder to create
     * @return the created folder
     */
    public MockFolder create(MockFolder folder) throws MockartyException {
        return client.post("/api/v1/mock-folders", folder, MockFolder.class);
    }

    /**
     * Updates an existing mock folder.
     *
     * @param id     the folder ID
     * @param folder the updated folder data
     * @return the updated folder
     */
    public MockFolder update(String id, MockFolder folder) throws MockartyException {
        return client.put("/api/v1/mock-folders/" + encode(id), folder, MockFolder.class);
    }

    /**
     * Deletes a mock folder.
     *
     * @param id the folder ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/mock-folders/" + encode(id));
    }

    /**
     * Moves a folder under a new parent.
     *
     * @param id       the folder ID to move
     * @param parentId the new parent folder ID (null for root)
     */
    public void move(String id, String parentId) throws MockartyException {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("parentId", parentId);
        client.patch("/api/v1/mock-folders/" + encode(id) + "/move", body);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
