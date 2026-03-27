// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Mock;
import ru.mockarty.model.UndefinedRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for managing undefined (unmatched) requests.
 */
public class UndefinedApi {

    private final MockartyClient client;

    public UndefinedApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all undefined requests.
     *
     * @return list of undefined requests
     */
    public List<UndefinedRequest> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, UndefinedRequest.class);
        return client.get("/api/v1/undefined-requests", listType);
    }

    /**
     * Marks an undefined request as ignored.
     *
     * @param id the undefined request ID
     */
    public void ignore(String id) throws MockartyException {
        client.patch("/api/v1/undefined-requests/" + encode(id) + "/ignore", null);
    }

    /**
     * Deletes specific undefined requests by IDs.
     *
     * @param ids the IDs to delete
     */
    public void delete(List<String> ids) throws MockartyException {
        client.delete("/api/v1/undefined-requests", Map.of("ids", ids));
    }

    /**
     * Clears all undefined requests.
     */
    public void clearAll() throws MockartyException {
        client.delete("/api/v1/undefined-requests/all");
    }

    /**
     * Creates a mock from an undefined request.
     *
     * @param requestId the undefined request ID
     * @return the created mock
     */
    public Mock createMock(String requestId) throws MockartyException {
        return client.post("/api/v1/undefined-requests/" + encode(requestId) + "/create-mock",
                null, Mock.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
