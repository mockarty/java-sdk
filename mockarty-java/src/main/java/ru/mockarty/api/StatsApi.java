// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.util.Map;

/**
 * API for retrieving system statistics and status.
 */
public class StatsApi {

    private final MockartyClient client;

    public StatsApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Gets general system statistics.
     *
     * @return statistics map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStats() throws MockartyException {
        return client.get("/api/v1/stats", Map.class);
    }

    /**
     * Gets resource counts (mocks, namespaces, etc.).
     *
     * @return counts map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCounts() throws MockartyException {
        return client.get("/api/v1/counts", Map.class);
    }

    /**
     * Gets the current system status.
     *
     * @return status map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatus() throws MockartyException {
        return client.get("/api/v1/status", Map.class);
    }

    /**
     * Gets the list of enabled features.
     *
     * @return features map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFeatures() throws MockartyException {
        return client.get("/api/v1/features", Map.class);
    }
}
