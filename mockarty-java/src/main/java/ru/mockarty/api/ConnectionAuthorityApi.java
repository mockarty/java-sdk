// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Namespace-scoped immutable Connection Authority lifecycle. */
public final class ConnectionAuthorityApi {
    private final MockartyClient client;

    public ConnectionAuthorityApi(MockartyClient client) {
        if (client == null) throw new IllegalArgumentException("client is required");
        this.client = client;
    }

    public JsonNode create(JsonNode descriptor) throws MockartyException {
        ObjectNode body = normalized(descriptor, true);
        String namespace = namespace(descriptor);
        return client.post(base(namespace), body, JsonNode.class);
    }

    public JsonNode getCurrent(String namespace, String connectionId) throws MockartyException {
        return client.get(path(namespace, connectionId), JsonNode.class);
    }

    public JsonNode advance(String namespace, String connectionId, JsonNode descriptor, long expectedRevision)
            throws MockartyException {
        if (expectedRevision <= 0) throw new IllegalArgumentException("expectedRevision must be positive");
        return client.put(path(namespace, connectionId) + "?expectedRevision=" + expectedRevision,
                normalized(descriptor, false), JsonNode.class);
    }

    public void revoke(String namespace, String connectionId, long revision) throws MockartyException {
        if (revision <= 0) throw new IllegalArgumentException("revision must be positive");
        client.delete(path(namespace, connectionId) + "?revision=" + revision);
    }

    private ObjectNode normalized(JsonNode descriptor, boolean keepId) {
        if (descriptor == null || !descriptor.isObject()) throw new IllegalArgumentException("descriptor is required");
        ObjectNode body = descriptor.deepCopy();
        body.remove("namespace");
        body.remove("revision");
        if (!keepId) body.remove("id");
        return body;
    }

    private String namespace(JsonNode descriptor) {
        String value = descriptor == null ? "" : descriptor.path("namespace").asText();
        if (value.isBlank()) value = client.getConfig().getNamespace();
        return requireNamespace(value);
    }

    private static String base(String namespace) {
        return "/api/v1/namespaces/" + encode(requireNamespace(namespace)) + "/connections";
    }

    private static String path(String namespace, String connectionId) {
        if (connectionId == null || connectionId.isBlank()) throw new IllegalArgumentException("connectionId is required");
        return base(namespace) + "/" + encode(connectionId);
    }

    private static String requireNamespace(String namespace) {
        if (namespace == null || namespace.isBlank() || namespace.equals("*")) {
            throw new IllegalArgumentException("a concrete namespace is required");
        }
        return namespace;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
