// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.LLMSecurityPolicyRequest;
import ru.mockarty.model.LLMSecurityPolicyResponse;
import ru.mockarty.model.LLMSecurityEventsResponse;
import ru.mockarty.model.LLMSecuritySandboxRequest;
import ru.mockarty.model.LLMSecuritySandboxResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Layered prompt-security policy management and local sandbox operations. */
public class LLMSecurityApi {
    private static final String ADMIN_PATH = "/api/v1/admin/llm-security/policy";
    private final MockartyClient client;

    public LLMSecurityApi(MockartyClient client) { this.client = client; }

    public LLMSecurityPolicyResponse getNamespacePolicy() throws MockartyException {
        return getNamespacePolicy(null);
    }

    public LLMSecurityPolicyResponse getNamespacePolicy(String namespace) throws MockartyException {
        return client.get(namespacePath(namespace) + "/policy", LLMSecurityPolicyResponse.class);
    }

    public LLMSecurityEventsResponse listNamespaceEvents(String namespace, int limit) throws MockartyException {
        requireLimit(limit);
        return client.get(namespacePath(namespace) + "/events?limit=" + limit, LLMSecurityEventsResponse.class);
    }

    public LLMSecurityPolicyResponse saveNamespacePolicy(String namespace, LLMSecurityPolicyRequest request)
            throws MockartyException {
        requirePolicyRequest(request);
        return client.put(namespacePath(namespace) + "/policy", request, LLMSecurityPolicyResponse.class);
    }

    public LLMSecurityPolicyResponse previewNamespacePolicy(String namespace, LLMSecurityPolicyRequest request)
            throws MockartyException {
        requirePolicyRequest(request);
        return client.post(namespacePath(namespace) + "/preview", request, LLMSecurityPolicyResponse.class);
    }

    public LLMSecuritySandboxResponse testNamespaceText(String namespace, LLMSecuritySandboxRequest request)
            throws MockartyException {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new IllegalArgumentException("sandbox text is required");
        }
        return client.post(namespacePath(namespace) + "/sandbox", request, LLMSecuritySandboxResponse.class);
    }

    public LLMSecurityPolicyResponse getInstallationPolicy() throws MockartyException {
        return client.get(ADMIN_PATH, LLMSecurityPolicyResponse.class);
    }

    public LLMSecurityPolicyResponse saveInstallationPolicy(LLMSecurityPolicyRequest request)
            throws MockartyException {
        requirePolicyRequest(request);
        return client.put(ADMIN_PATH, request, LLMSecurityPolicyResponse.class);
    }

    public LLMSecurityEventsResponse listInstallationEvents(int limit) throws MockartyException {
        requireLimit(limit);
        return client.get("/api/v1/admin/llm-security/events?limit=" + limit, LLMSecurityEventsResponse.class);
    }

    private String namespacePath(String namespace) {
        String resolved = namespace == null || namespace.isBlank()
                ? client.getConfig().getNamespace() : namespace.trim();
        return "/api/v1/namespaces/" + pathSegment(resolved) + "/llm-security";
    }

    private static void requirePolicyRequest(LLMSecurityPolicyRequest request) {
        if (request == null || request.getDocument() == null || request.getExpectedRevision() < 0) {
            throw new IllegalArgumentException("policy document and non-negative expected revision are required");
        }
    }

    private static void requireLimit(int limit) {
        if (limit < 1 || limit > 500) {
            throw new IllegalArgumentException("limit must be between 1 and 500");
        }
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
