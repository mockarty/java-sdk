// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Environment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * API for API tester environment management.
 */
public class EnvironmentApi {

    private final MockartyClient client;

    public EnvironmentApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all environments.
     *
     * @return list of environments
     */
    public List<Environment> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, Environment.class);
        return client.get("/api/v1/api-tester/environments", listType);
    }

    /**
     * Gets the currently active environment.
     *
     * @return the active environment
     */
    public Environment getActive() throws MockartyException {
        return client.get("/api/v1/api-tester/environments/active", Environment.class);
    }

    /**
     * Gets an environment by ID.
     *
     * @param id the environment ID
     * @return the environment
     */
    public Environment get(String id) throws MockartyException {
        return client.get("/api/v1/api-tester/environments/" + encode(id), Environment.class);
    }

    /**
     * Creates a new environment.
     *
     * @param environment the environment to create
     * @return the created environment
     */
    public Environment create(Environment environment) throws MockartyException {
        return client.post("/api/v1/api-tester/environments", environment, Environment.class);
    }

    /**
     * Updates an existing environment.
     *
     * @param id          the environment ID
     * @param environment the updated environment data
     * @return the updated environment
     */
    public Environment update(String id, Environment environment) throws MockartyException {
        return client.put("/api/v1/api-tester/environments/" + encode(id), environment, Environment.class);
    }

    /**
     * Deletes an environment.
     *
     * @param id the environment ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/api-tester/environments/" + encode(id));
    }

    /**
     * Activates an environment (sets it as the current active environment).
     *
     * @param id the environment ID to activate
     */
    public void activate(String id) throws MockartyException {
        client.post("/api/v1/api-tester/environments/" + encode(id) + "/activate", null);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
