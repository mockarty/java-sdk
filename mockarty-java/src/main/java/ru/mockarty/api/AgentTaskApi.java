// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.AgentTask;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.time.Duration;

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
     * <p>Wire shape: server emits {@code {tasks:[...], total, limit, offset}}
     * envelope — NOT a bare list. Older SDK builds tried to decode the
     * envelope object into {@code List<AgentTask>} and threw on every call.
     *
     * @return list of agent tasks
     */
    @SuppressWarnings("unchecked")
    public List<AgentTask> list() throws MockartyException {
        Map<String, Object> env = client.get("/api/v1/agent/tasks", Map.class);
        if (env == null) {
            return java.util.Collections.emptyList();
        }
        Object raw = env.get("tasks");
        if (!(raw instanceof List)) {
            return java.util.Collections.emptyList();
        }
        return client.getObjectMapper().convertValue(raw,
                client.getObjectMapper().getTypeFactory()
                        .constructCollectionType(List.class, AgentTask.class));
    }

    /**
     * Gets a specific agent task by ID.
     *
     * <p>Wire shape: server emits {@code {task: <AgentTask>}} envelope.
     *
     * @param id the task ID
     * @return the agent task
     */
    @SuppressWarnings("unchecked")
    public AgentTask get(String id) throws MockartyException {
        Map<String, Object> env = client.get(
                "/api/v1/agent/tasks/" + encode(id), Map.class);
        if (env == null) return null;
        Object raw = env.get("task");
        if (raw == null) return null;
        return client.getObjectMapper().convertValue(raw, AgentTask.class);
    }

    /**
     * Submits a new agent task.
     *
     * <p>Server requires {@code title} + {@code prompt} fields. Wire reply:
     * {@code {task: <AgentTask>, message: "..."}} envelope — unwrap.
     *
     * @param task the task parameters
     * @return the created task
     */
    @SuppressWarnings("unchecked")
    public AgentTask submit(Map<String, Object> task) throws MockartyException {
        Map<String, Object> env = client.post(
                "/api/v1/agent/tasks", task, Map.class);
        if (env == null) return null;
        Object raw = env.get("task");
        if (raw == null) return null;
        return client.getObjectMapper().convertValue(raw, AgentTask.class);
    }

    /**
     * Cancels a running agent task.
     *
     * @param id the task ID to cancel
     */
    public void cancel(String id) throws MockartyException {
        client.post("/api/v1/agent/tasks/" + encode(id) + "/cancel", null);
    }

    /**
     * Deletes an agent task.
     *
     * @param id the task ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/agent/tasks/" + encode(id));
    }

    /**
     * Clears all agent tasks.
     */
    public void clearAll() throws MockartyException {
        client.delete("/api/v1/agent/tasks");
    }

    /**
     * Re-runs an agent task.
     *
     * <p>Wire shape: server emits {@code {task: <AgentTask>, message: "..."}}
     * envelope — unwrap.
     *
     * @param id the task ID to re-run
     * @return the new task
     */
    @SuppressWarnings("unchecked")
    public AgentTask rerun(String id) throws MockartyException {
        Map<String, Object> env = client.post(
                "/api/v1/agent/tasks/" + encode(id) + "/rerun", null, Map.class);
        if (env == null) return null;
        Object raw = env.get("task");
        if (raw == null) return null;
        return client.getObjectMapper().convertValue(raw, AgentTask.class);
    }

    /**
     * Exports an agent task result as bytes.
     *
     * @param id the task ID
     * @return the exported data
     */
    public byte[] export(String id) throws MockartyException {
        return client.getBytes("/api/v1/agent/tasks/" + encode(id) + "/export");
    }

    /**
     * Polls a task until it reaches a terminal state, returning the finished
     * task (with its result). Throws {@link MockartyException} on a
     * {@code failed} / {@code cancelled} terminal state. Automation counterpart
     * to {@link #submit(Map)} — dispatch into the agent network and block for a
     * result without hand-rolling a poll loop.
     *
     * @param id           the task ID to poll
     * @param pollInterval interval between polls; {@code null} or non-positive → 2s
     * @return the completed task
     * @throws MockartyException if the task fails, is cancelled, or the wait is interrupted
     */
    public AgentTask waitForResult(String id, Duration pollInterval) throws MockartyException {
        long millis = (pollInterval == null || pollInterval.toMillis() <= 0)
                ? 2000L : pollInterval.toMillis();
        while (true) {
            AgentTask task = get(id);
            String status = task == null || task.getStatus() == null
                    ? "" : task.getStatus().toLowerCase();
            switch (status) {
                case "completed":
                case "done":
                case "succeeded":
                    return task;
                case "failed":
                case "error":
                    throw new MockartyException("agent task " + id + " failed");
                case "cancelled":
                case "canceled":
                    throw new MockartyException("agent task " + id + " cancelled");
                default:
                    break;
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MockartyException("interrupted while waiting for agent task " + id, e);
            }
        }
    }

    /**
     * Submits a task and blocks until it reaches a terminal state — the
     * one-call entry point for "run this in the agent network, give me the
     * result".
     *
     * @param task         the task parameters ({@code title} + {@code prompt} required)
     * @param pollInterval interval between polls; {@code null} or non-positive → 2s
     * @return the completed task
     * @throws MockartyException if submission fails or the task ends unsuccessfully
     */
    public AgentTask submitAndWait(Map<String, Object> task, Duration pollInterval) throws MockartyException {
        AgentTask submitted = submit(task);
        if (submitted == null || submitted.getId() == null || submitted.getId().isEmpty()) {
            throw new MockartyException("agent task submitted without an id");
        }
        return waitForResult(submitted.getId(), pollInterval);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
