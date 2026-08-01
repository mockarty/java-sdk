// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Git-sync API — bind an autotest collection (API + UI tests) to a git
 * repository. Pull materialises the repo tree into Mockarty; push writes local
 * edits back. Git I/O runs server-side (go-git); the SDK just orchestrates.
 * Great from CI: pull the team's autotests and run them, or push what you
 * recorded. The auth token is write-only — responses never carry it.
 */
public class GitSyncApi {

    private final MockartyClient client;

    public GitSyncApi(MockartyClient client) {
        this.client = client;
    }

    private String ns() {
        String namespace = client.getConfig().getNamespace();
        if (namespace == null || namespace.isEmpty()) {
            return "";
        }
        return "?namespace=" + enc(namespace);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Bind a repo (POST /api/v1/git-sync/bindings). Fields: repoUrl (required),
     *  branch, subdir, kind (api|ui|mixed), authUsername, authToken (write-only),
     *  collectionId, enabled, autoSync. Returns the stored binding (no token). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> createBinding(Map<String, Object> input) throws MockartyException {
        return client.post("/api/v1/git-sync/bindings" + ns(), input, Map.class);
    }

    /** Convenience: bind a repo with the common fields. */
    public Map<String, Object> createBinding(String repoUrl, String branch, String subdir,
                                             String kind, String authUsername, String authToken,
                                             boolean autoSync) throws MockartyException {
        Map<String, Object> in = new HashMap<>();
        in.put("repoUrl", repoUrl);
        if (branch != null && !branch.isEmpty()) in.put("branch", branch);
        if (subdir != null && !subdir.isEmpty()) in.put("subdir", subdir);
        if (kind != null && !kind.isEmpty()) in.put("kind", kind);
        if (authUsername != null && !authUsername.isEmpty()) in.put("authUsername", authUsername);
        if (authToken != null && !authToken.isEmpty()) in.put("authToken", authToken);
        in.put("autoSync", autoSync);
        return createBinding(in);
    }

    /** List the namespace's bindings. */
    public List<Map<String, Object>> listBindings() throws MockartyException {
        JsonNode root = client.get("/api/v1/git-sync/bindings" + ns(), JsonNode.class);
        JsonNode arr = root != null && root.isObject() ? root.path("bindings") : root;
        List<Map<String, Object>> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            JavaType t = client.getObjectMapper().getTypeFactory()
                    .constructMapType(java.util.LinkedHashMap.class, String.class, Object.class);
            for (JsonNode n : arr) {
                out.add(client.getObjectMapper().convertValue(n, t));
            }
        }
        return out;
    }

    /** Get a binding (with last-sync status) by id. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBinding(String id) throws MockartyException {
        return client.get("/api/v1/git-sync/bindings/" + enc(id) + ns(), Map.class);
    }

    /** Remove a binding (already-synced tests stay in Mockarty). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> deleteBinding(String id) throws MockartyException {
        return client.delete("/api/v1/git-sync/bindings/" + enc(id) + ns(), null, Map.class);
    }

    /** Clone + materialise the tests. Returns {commit, uiTestsFound}. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pull(String id) throws MockartyException {
        return client.post("/api/v1/git-sync/bindings/" + enc(id) + "/pull" + ns(), null, Map.class);
    }

    /** Serialise the namespace's tests + commit + push. Returns {commit}. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> push(String id, String message) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/git-sync/bindings/").append(enc(id)).append("/push");
        String namespace = client.getConfig().getNamespace();
        boolean hasQ = false;
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(enc(namespace));
            hasQ = true;
        }
        if (message != null && !message.isEmpty()) {
            path.append(hasQ ? "&" : "?").append("message=").append(enc(message));
        }
        return client.post(path.toString(), null, Map.class);
    }
}
