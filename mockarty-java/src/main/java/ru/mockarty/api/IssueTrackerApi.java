// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Issue-tracker task-automation API — create/read/update/transition issues,
 * comment, search, claim the next issue, and manage projects/sprints over
 * Mockarty's built-in tracker. Issue payloads are rich and evolve, so this API
 * uses loosely-typed {@link JsonNode} / {@link Map} I/O (mirrored by the Go map
 * and Python dict SDKs). Every method takes a {@code namespace} argument;
 * pass {@code null}/empty to use the client default.
 */
public class IssueTrackerApi {

    private final MockartyClient client;

    public IssueTrackerApi(MockartyClient client) {
        this.client = client;
    }

    private String base(String namespace) {
        String ns = (namespace == null || namespace.isEmpty())
                ? client.getConfig().getNamespace() : namespace;
        if (ns == null || ns.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        return "/api/v1/namespaces/" + ns + "/issuetracker";
    }

    /** Create an issue (fields: {@code projectId}, {@code type}, {@code title}, …). */
    public JsonNode createIssue(String namespace, Map<String, Object> issue) throws MockartyException {
        return client.post(base(namespace) + "/issues", issue, JsonNode.class);
    }

    /** Fetch an issue by its UUID. */
    public JsonNode getIssue(String namespace, String issueId) throws MockartyException {
        return client.get(base(namespace) + "/issues/" + enc(issueId), JsonNode.class);
    }

    /** Fetch an issue by its human key (e.g. {@code MK-42}). */
    public JsonNode getIssueByKey(String namespace, String key) throws MockartyException {
        return client.get(base(namespace) + "/issues/by-key/" + enc(key), JsonNode.class);
    }

    /** List issues, optionally filtered via query params (e.g. {@code status=open}). */
    public List<JsonNode> listIssues(String namespace, Map<String, String> filters) throws MockartyException {
        JsonNode data = client.get(base(namespace) + "/issues" + query(filters), JsonNode.class);
        return arrayField(data, "issues");
    }

    /** Text-search over issues. */
    public List<JsonNode> searchIssues(String namespace, String query) throws MockartyException {
        Map<String, String> p = new HashMap<>();
        p.put("q", query);
        JsonNode data = client.get(base(namespace) + "/issues/search" + query(p), JsonNode.class);
        return arrayField(data, "issues");
    }

    /** Return the next issue available to work on (agent claim flow). */
    public JsonNode nextIssue(String namespace, Map<String, String> params) throws MockartyException {
        return client.get(base(namespace) + "/issues/next" + query(params), JsonNode.class);
    }

    /** Apply a partial update to an issue. */
    public JsonNode updateIssue(String namespace, String issueId, Map<String, Object> fields) throws MockartyException {
        return client.put(base(namespace) + "/issues/" + enc(issueId), fields, JsonNode.class);
    }

    /**
     * Transition an issue to a new workflow status. {@code resolution} is
     * required when moving into a terminal (closed) status; pass {@code null}/empty.
     */
    public JsonNode moveIssue(String namespace, String issueId, String status, String resolution) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        if (resolution != null && !resolution.isEmpty()) {
            body.put("resolution", resolution);
        }
        return client.post(base(namespace) + "/issues/" + enc(issueId) + "/move", body, JsonNode.class);
    }

    /** Soft-delete an issue. */
    public void deleteIssue(String namespace, String issueId) throws MockartyException {
        client.delete(base(namespace) + "/issues/" + enc(issueId));
    }

    /** Post a comment to an issue. */
    public JsonNode addComment(String namespace, String issueId, String body) throws MockartyException {
        return client.post(base(namespace) + "/issues/" + enc(issueId) + "/comments",
                Map.of("body", body), JsonNode.class);
    }

    /** List an issue's comments. */
    public List<JsonNode> listComments(String namespace, String issueId) throws MockartyException {
        JsonNode data = client.get(base(namespace) + "/issues/" + enc(issueId) + "/comments", JsonNode.class);
        return arrayField(data, "comments");
    }

    /** Assign many issues to one assignee in a single call. */
    public void bulkAssign(String namespace, List<String> issueIds, String assigneeId) throws MockartyException {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", issueIds);
        body.put("assigneeId", assigneeId);
        client.post(base(namespace) + "/issues/bulk/assign", body);
    }

    /** List the tracker's projects. */
    public List<JsonNode> listProjects(String namespace) throws MockartyException {
        return arrayField(client.get(base(namespace) + "/projects", JsonNode.class), "projects");
    }

    /** Create a project. */
    public JsonNode createProject(String namespace, Map<String, Object> project) throws MockartyException {
        return client.post(base(namespace) + "/projects", project, JsonNode.class);
    }

    /** List sprints, optionally filtered (e.g. {@code projectId=...}). */
    public List<JsonNode> listSprints(String namespace, Map<String, String> filters) throws MockartyException {
        return arrayField(client.get(base(namespace) + "/sprints" + query(filters), JsonNode.class), "sprints");
    }

    /** Create a sprint. */
    public JsonNode createSprint(String namespace, Map<String, Object> sprint) throws MockartyException {
        return client.post(base(namespace) + "/sprints", sprint, JsonNode.class);
    }

    private static List<JsonNode> arrayField(JsonNode data, String field) {
        List<JsonNode> out = new ArrayList<>();
        if (data != null && data.path(field).isArray()) {
            for (JsonNode n : data.path(field)) {
                out.add(n);
            }
        }
        return out;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Render a "?k=v&..." string (sorted for stable output); "" when empty. */
    private static String query(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            sb.append(sb.length() == 0 ? "?" : "&")
                    .append(enc(e.getKey())).append("=").append(enc(e.getValue()));
        }
        return sb.toString();
    }
}
