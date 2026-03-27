// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Tag;

import java.util.List;
import java.util.Map;

/**
 * API for tag management operations.
 */
public class TagApi {

    private final MockartyClient client;

    public TagApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all tags.
     *
     * @return list of tags
     */
    public List<Tag> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Tag.class);
        return client.get("/api/v1/tags", listType);
    }

    /**
     * Creates a new tag.
     *
     * @param name the tag name
     * @return the created tag
     */
    public Tag create(String name) throws MockartyException {
        return client.post("/api/v1/tags", Map.of("name", name), Tag.class);
    }
}
