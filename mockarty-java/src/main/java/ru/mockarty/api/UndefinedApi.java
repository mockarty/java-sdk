// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

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
     * Auto-generates a mock from a recorded undefined request.
     *
     * <p>Targets {@code /convert} (server derives the mock from the
     * stored row, protocol auto-detected) and unwraps the
     * {@code {mock, mockId, protocol}} envelope. The older
     * {@code /create-mock} path required a caller-supplied
     * {@code {mockData}} body and 400'd on a bare call.
     *
     * @param requestId the undefined request ID
     * @return the auto-generated mock
     */
    @SuppressWarnings("unchecked")
    public Mock createMock(String requestId) throws MockartyException {
        Map<String, Object> env = client.post(
                "/api/v1/undefined-requests/" + encode(requestId) + "/convert", null, Map.class);
        if (env == null) {
            return null;
        }
        Object raw = env.get("mock");
        if (raw == null) {
            return null;
        }
        return client.getObjectMapper().convertValue(raw, Mock.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
