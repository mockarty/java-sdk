// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned workflow draft, dry-run and immutable-publish lifecycle. */
public final class WorkflowDefinitionsApi {
    private final MockartyClient client;

    public WorkflowDefinitionsApi(MockartyClient client) {
        if (client == null) throw new IllegalArgumentException("client is required");
        this.client = client;
    }

    public JsonNode list(String namespace, String workflowId, String status, String cursor, int limit)
            throws MockartyException {
        StringBuilder path = new StringBuilder(base(namespace));
        Map<String, String> query = new LinkedHashMap<>();
        if (workflowId != null && !workflowId.isBlank()) query.put("id", workflowId);
        if (status != null && !status.isBlank()) query.put("status", status);
        if (cursor != null && !cursor.isBlank()) query.put("cursor", cursor);
        if (limit > 0) query.put("limit", Integer.toString(limit));
        if (!query.isEmpty()) {
            path.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) path.append('&');
                path.append(enc(entry.getKey())).append('=').append(enc(entry.getValue()));
                first = false;
            }
        }
        return client.get(path.toString(), JsonNode.class);
    }

    public JsonNode createDraft(JsonNode definition) throws MockartyException {
        ObjectNode normalized = normalizedDefinition(definition);
        return client.post(base(normalized.path("namespace").asText()), normalized, JsonNode.class);
    }

    public JsonNode get(String namespace, String workflowId, String version) throws MockartyException {
        return client.get(versionPath(namespace, workflowId, version), JsonNode.class);
    }

    public JsonNode updateDraft(JsonNode definition, long expectedRevision) throws MockartyException {
        ObjectNode normalized = normalizedDefinition(definition);
        if (expectedRevision <= 0) throw new IllegalArgumentException("expectedRevision must be positive");
        Map<String, Object> body = Map.of("definition", normalized, "expectedRevision", expectedRevision);
        return client.put(versionPath(normalized.path("namespace").asText(), normalized.path("id").asText(),
                normalized.path("version").asText()), body, JsonNode.class);
    }

    public JsonNode dryRun(String namespace, String workflowId, String version, long expectedRevision)
            throws MockartyException {
        return postRevision(versionPath(namespace, workflowId, version) + "/dry-run", expectedRevision);
    }

    public JsonNode publish(String namespace, String workflowId, String version, long expectedRevision)
            throws MockartyException {
        return postRevision(versionPath(namespace, workflowId, version) + "/publish", expectedRevision);
    }

    private JsonNode postRevision(String path, long expectedRevision) throws MockartyException {
        if (expectedRevision <= 0) throw new IllegalArgumentException("expectedRevision must be positive");
        return client.post(path, Map.of("expectedRevision", expectedRevision), JsonNode.class);
    }

    private ObjectNode normalizedDefinition(JsonNode definition) {
        if (definition == null || !definition.isObject()) throw new IllegalArgumentException("definition is required");
        ObjectNode normalized = definition.deepCopy();
        if (normalized.path("namespace").asText().isBlank()) {
            normalized.put("namespace", client.getConfig().getNamespace());
        }
        if (normalized.path("namespace").asText().isBlank() || normalized.path("id").asText().isBlank()
                || normalized.path("version").asText().isBlank()) {
            throw new IllegalArgumentException("definition namespace, id and version are required");
        }
        return normalized;
    }

    private static String base(String namespace) {
        if (namespace == null || namespace.isBlank() || namespace.equals("*")) {
            throw new IllegalArgumentException("a concrete namespace is required");
        }
        return "/api/v1/namespaces/" + enc(namespace) + "/workflow-definitions";
    }

    private static String versionPath(String namespace, String workflowId, String version) {
        if (workflowId == null || workflowId.isBlank() || version == null || version.isBlank()) {
            throw new IllegalArgumentException("workflowId and version are required");
        }
        return base(namespace) + "/" + enc(workflowId) + "/versions/" + enc(version);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
