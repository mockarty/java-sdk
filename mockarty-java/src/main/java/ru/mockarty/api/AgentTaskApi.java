// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.AgentTask;
import ru.mockarty.model.ToolReceipt;

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
        AgentTask task = client.getObjectMapper().convertValue(raw, AgentTask.class);
        Object receipts = env.get("toolReceipts");
        if (receipts != null) {
            task.toolReceipts(client.getObjectMapper().convertValue(receipts,
                    client.getObjectMapper().getTypeFactory()
                            .constructCollectionType(List.class, ToolReceipt.class)));
        }
        Object canReconcile = env.get("canReconcileToolReceipts");
        if (canReconcile instanceof Boolean) {
            task.canReconcileToolReceipts((Boolean) canReconcile);
        }
        Object retryAllowed = env.get("toolReceiptRetryAllowed");
        if (retryAllowed instanceof Boolean) {
            task.toolReceiptRetryAllowed((Boolean) retryAllowed);
        }
        Object blockedReason = env.get("toolReceiptReconcileBlockedReason");
        if (blockedReason instanceof String) {
            task.toolReceiptReconcileBlockedReason((String) blockedReason);
        }
        return task;
    }

    /**
     * Resolves one uncertain external action after inspecting the real target.
     * Decision is {@code already_applied}, {@code retry_once}, or
     * {@code mark_failed}. Reuse the same idempotency key when retrying this
     * request. Reason is limited to 2000 encoded UTF-8 bytes and result to
     * 65536 encoded UTF-8 bytes; retry_once authorizes exactly one new physical
     * dispatch.
     */
    @SuppressWarnings("unchecked")
    public ToolReceipt reconcileToolReceipt(
            String taskId,
            String receiptKey,
            long expectedVersion,
            String idempotencyKey,
            String decision,
            String reason,
            String result) throws MockartyException {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("expectedVersion", expectedVersion);
        request.put("idempotencyKey", idempotencyKey);
        request.put("decision", decision);
        request.put("reason", reason);
        request.put("result", result == null ? "" : result);
        Map<String, Object> envelope = client.post(
                "/api/v1/agent/tasks/" + encode(taskId) + "/tool-receipts/" +
                        encode(receiptKey) + "/reconcile",
                request, Map.class);
        if (envelope == null || envelope.get("receipt") == null) return null;
        return client.getObjectMapper().convertValue(envelope.get("receipt"), ToolReceipt.class);
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
