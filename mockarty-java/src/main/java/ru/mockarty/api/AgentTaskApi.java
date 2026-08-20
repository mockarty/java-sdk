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
     * Lists an owner-scoped page of recoverable pre-namespace sessions.
     *
     * @param limit bounded page size from 1 through 100
     * @param cursor opaque cursor from the previous response, or null
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listLegacySessions(int limit, String cursor) throws MockartyException {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("limit must be between 1 and 100");
        }
        StringBuilder path = new StringBuilder("/api/v1/agent/sessions/legacy?limit=")
                .append(limit);
        if (cursor != null && !cursor.isEmpty()) {
            path.append("&cursor=").append(encode(cursor));
        }
        return client.get(path.toString(), Map.class);
    }

    /** Returns one bounded page of a recoverable transcript. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> exportLegacySession(String id, int limit, long afterEventId)
            throws MockartyException {
        requireLegacySessionId(id);
        if (limit < 1 || limit > 2000) {
            throw new IllegalArgumentException("limit must be between 1 and 2000");
        }
        if (afterEventId < 0) {
            throw new IllegalArgumentException("afterEventId must be non-negative");
        }
        String path = "/api/v1/agent/sessions/legacy/" + encode(id)
                + "/export?limit=" + limit + "&afterEventId=" + afterEventId;
        return client.get(path, Map.class);
    }

    /**
     * Claims a recoverable transcript into a write-authorized workspace.
     * acknowledgeUnknownOrigin must be true; sessionKey may be null to reuse
     * the legacy public key.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> claimLegacySession(
            String id,
            String namespace,
            String sessionKey,
            boolean acknowledgeUnknownOrigin
    ) throws MockartyException {
        requireLegacySessionId(id);
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (!acknowledgeUnknownOrigin) {
            throw new IllegalArgumentException("acknowledgeUnknownOrigin must be true");
        }
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("namespace", namespace);
        request.put("acknowledgeUnknownOrigin", acknowledgeUnknownOrigin);
        if (sessionKey != null && !sessionKey.isEmpty()) {
            request.put("sessionKey", sessionKey);
        }
        Map<String, Object> envelope = client.post(
                "/api/v1/agent/sessions/legacy/" + encode(id) + "/claim",
                request,
                Map.class);
        if (envelope == null || !(envelope.get("session") instanceof Map)) {
            return java.util.Collections.emptyMap();
        }
        return (Map<String, Object>) envelope.get("session");
    }

    private static void requireLegacySessionId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("legacy session id is required");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
