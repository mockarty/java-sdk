// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.Mock;
import ru.mockarty.model.SaveMockResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test helper that tracks created mocks for automatic cleanup.
 * Used in conjunction with {@link MockartyExtension} to ensure test isolation.
 *
 * <p>All mocks created through this server are automatically tracked and can be
 * cleaned up after each test by calling {@link #cleanup()}.</p>
 */
public class MockartyServer {

    private static final Logger log = LoggerFactory.getLogger(MockartyServer.class);

    private final MockartyClient client;
    private final List<String> createdMockIds = Collections.synchronizedList(new ArrayList<>());

    /**
     * Creates a new MockartyServer wrapping the given client.
     *
     * @param client the Mockarty client to use for API calls
     */
    public MockartyServer(MockartyClient client) {
        this.client = client;
    }

    /**
     * Creates a mock on the server and tracks it for automatic cleanup.
     *
     * @param mock the mock to create
     * @return the created mock (with server-generated fields populated)
     * @throws MockartyException if the API call fails
     */
    public Mock createMock(Mock mock) throws MockartyException {
        SaveMockResponse response = client.mocks().create(mock);
        String mockId = response.getMock().getId();
        createdMockIds.add(mockId);
        log.debug("Created and tracked mock: {}", mockId);
        return response.getMock();
    }

    /**
     * Creates a mock on the server and tracks it for automatic cleanup.
     *
     * @param mock the mock to create
     * @return the save response
     * @throws MockartyException if the API call fails
     */
    public SaveMockResponse createMockRaw(Mock mock) throws MockartyException {
        SaveMockResponse response = client.mocks().create(mock);
        String mockId = response.getMock().getId();
        createdMockIds.add(mockId);
        log.debug("Created and tracked mock: {}", mockId);
        return response;
    }

    /**
     * Returns the IDs of all mocks created through this server.
     *
     * @return unmodifiable list of created mock IDs
     */
    public List<String> getCreatedMockIds() {
        return Collections.unmodifiableList(createdMockIds);
    }

    /**
     * Returns the number of mocks created through this server.
     *
     * @return the count of tracked mocks
     */
    public int createdMockCount() {
        return createdMockIds.size();
    }

    /**
     * Returns the underlying Mockarty client.
     *
     * @return the client
     */
    public MockartyClient getClient() {
        return client;
    }

    /**
     * Deletes all mocks that were created through this server.
     * Errors during deletion are logged but do not cause failures.
     */
    public void cleanup() {
        if (createdMockIds.isEmpty()) {
            log.debug("No mocks to clean up");
            return;
        }

        log.debug("Cleaning up {} tracked mocks", createdMockIds.size());
        List<String> failedIds = new ArrayList<>();

        for (String mockId : createdMockIds) {
            try {
                client.mocks().delete(mockId);
                log.trace("Deleted mock: {}", mockId);
            } catch (MockartyException e) {
                log.warn("Failed to delete mock {}: {}", mockId, e.getMessage());
                failedIds.add(mockId);
            }
        }

        int cleaned = createdMockIds.size() - failedIds.size();
        createdMockIds.clear();

        if (!failedIds.isEmpty()) {
            log.warn("Cleanup completed: {}/{} mocks deleted, {} failed: {}",
                    cleaned, cleaned + failedIds.size(), failedIds.size(), failedIds);
        } else {
            log.debug("Cleanup completed: {} mocks deleted", cleaned);
        }
    }
}
