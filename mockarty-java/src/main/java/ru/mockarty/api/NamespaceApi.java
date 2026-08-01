// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.util.Collections;
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

    // copyMocks removed — it POSTed to a non-existent
    // /api/v1/namespaces/copy-mocks route (404). Use
    // mocks().copyToNamespace(mockIds, target) for the real operation.

    /**
     * Lists all available namespaces.
     *
     * <p>The admin server returns the list inside an envelope:
     * <pre>{"namespaces": ["sandbox", ...]}</pre>
     * We decode the envelope and surface the bare list so callers don't
     * have to know about the wire shape.</p>
     *
     * @return list of namespace names (never {@code null})
     */
    public List<String> list() throws MockartyException {
        NamespaceListResponse env = client.get("/api/v1/namespaces", NamespaceListResponse.class);
        return env == null || env.namespaces == null
                ? Collections.emptyList()
                : env.namespaces;
    }

    /** Envelope DTO matching {@code {"namespaces": ["...", ...]}}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class NamespaceListResponse {
        @JsonProperty("namespaces")
        List<String> namespaces;
    }
}
