// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.AgentTask;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * API for AI agent task management.
 */
public class AgentTaskApi {

    private final MockartyClient client;

    public AgentTaskApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Lists all agent tasks.
     *
     * @return list of agent tasks
     */
    public List<AgentTask> list() throws MockartyException {
        JavaType listType = client.getObjectMapper().getTypeFactory()
                .constructCollectionType(List.class, AgentTask.class);
        return client.get("/api/v1/agent-tasks", listType);
    }

    /**
     * Gets a specific agent task by ID.
     *
     * @param id the task ID
     * @return the agent task
     */
    public AgentTask get(String id) throws MockartyException {
        return client.get("/api/v1/agent-tasks/" + encode(id), AgentTask.class);
    }

    /**
     * Submits a new agent task.
     *
     * @param task the task parameters
     * @return the created task
     */
    public AgentTask submit(Map<String, Object> task) throws MockartyException {
        return client.post("/api/v1/agent-tasks", task, AgentTask.class);
    }

    /**
     * Cancels a running agent task.
     *
     * @param id the task ID to cancel
     */
    public void cancel(String id) throws MockartyException {
        client.post("/api/v1/agent-tasks/" + encode(id) + "/cancel", null);
    }

    /**
     * Deletes an agent task.
     *
     * @param id the task ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/agent-tasks/" + encode(id));
    }

    /**
     * Clears all agent tasks.
     */
    public void clearAll() throws MockartyException {
        client.delete("/api/v1/agent-tasks");
    }

    /**
     * Re-runs an agent task.
     *
     * @param id the task ID to re-run
     * @return the new task
     */
    public AgentTask rerun(String id) throws MockartyException {
        return client.post("/api/v1/agent-tasks/" + encode(id) + "/rerun", null, AgentTask.class);
    }

    /**
     * Exports an agent task result as bytes.
     *
     * @param id the task ID
     * @return the exported data
     */
    public byte[] export(String id) throws MockartyException {
        return client.getBytes("/api/v1/agent-tasks/" + encode(id) + "/export");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
