// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.HealthResponse;

/**
 * API for health check operations.
 */
public class HealthApi {

    private final MockartyClient client;

    public HealthApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Performs a comprehensive health check.
     *
     * @return the health check response
     */
    public HealthResponse check() throws MockartyException {
        return client.get("/health", HealthResponse.class);
    }

    /**
     * Checks if the server is alive (liveness probe).
     *
     * @return true if the server is alive
     */
    public boolean live() {
        try {
            HealthResponse response = check();
            return response != null;
        } catch (MockartyException e) {
            return false;
        }
    }

    /**
     * Checks if the server is ready to accept traffic (readiness probe).
     *
     * @return true if the server is ready
     */
    public boolean ready() {
        try {
            HealthResponse response = check();
            return response != null && response.isHealthy();
        } catch (MockartyException e) {
            return false;
        }
    }

    /**
     * Gets the server version from the health endpoint.
     *
     * @return the server version string, or "unknown" if not available
     */
    public String version() {
        try {
            HealthResponse response = check();
            return response != null && response.getReleaseId() != null
                    ? response.getReleaseId()
                    : "unknown";
        } catch (MockartyException e) {
            return "unknown";
        }
    }
}
