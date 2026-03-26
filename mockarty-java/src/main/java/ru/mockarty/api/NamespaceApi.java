// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.util.List;
import java.util.Map;

/**
 * API for namespace management operations.
 */
public class NamespaceApi {

    private final MockartyClient client;

    public NamespaceApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Creates a new namespace.
     *
     * @param name the namespace name
     */
    public void create(String name) throws MockartyException {
        client.post("/api/v1/namespaces", Map.of("name", name));
    }

    /**
     * Lists all available namespaces.
     *
     * @return list of namespace names
     */
    public List<String> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, String.class);
        return client.get("/api/v1/namespaces", listType);
    }
}
