// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test Case Management (TCM) automation API — cases, case-runs, defects.
 *
 * <p>Create/read/update/run test cases, poll case-runs, and file defects.
 * Cases live under {@code /api/v1/namespaces/:ns/test-cases}; case-runs and
 * defects under {@code /api/v1/namespaces/:ns/tcm/...}. Payloads are rich and
 * evolve, so this API uses loosely-typed {@link JsonNode} / {@link Map} I/O
 * (mirrored by the Go map and Python dict SDKs). Every method takes a
 * {@code namespace}; pass {@code null}/empty to use the client default.
 */
public class TcmApi {

    private final MockartyClient client;

    public TcmApi(MockartyClient client) {
        this.client = client;
    }

    private String ns(String namespace) {
        String n = (namespace == null || namespace.isEmpty())
                ? client.getConfig().getNamespace() : namespace;
        if (n == null || n.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        return n;
    }

    private String casesBase(String namespace) {
        return "/api/v1/namespaces/" + ns(namespace) + "/test-cases";
    }

    private String tcmBase(String namespace) {
        return "/api/v1/namespaces/" + ns(namespace) + "/tcm";
    }

    // -- cases --

    /** Create a test case (fields: {@code title}, {@code folderId}, {@code steps}, …). */
    public JsonNode createCase(String namespace, Map<String, Object> testCase) throws MockartyException {
        return client.post(casesBase(namespace), testCase, JsonNode.class);
    }

    /** Fetch a test case by id. */
    public JsonNode getCase(String namespace, String caseId) throws MockartyException {
        return client.get(casesBase(namespace) + "/" + enc(caseId), JsonNode.class);
    }

    /** List test cases, optionally filtered via query params. */
    public List<JsonNode> listCases(String namespace, Map<String, String> filters) throws MockartyException {
        JsonNode data = client.get(casesBase(namespace) + query(filters), JsonNode.class);
        return array(data, "test_cases", "cases", "items");
    }

    /** Apply an update to a test case. */
    public JsonNode updateCase(String namespace, String caseId, Map<String, Object> fields) throws MockartyException {
        return client.put(casesBase(namespace) + "/" + enc(caseId), fields, JsonNode.class);
    }

    /** Soft-delete a test case. */
    public void deleteCase(String namespace, String caseId) throws MockartyException {
        client.delete(casesBase(namespace) + "/" + enc(caseId));
    }

    /**
     * Start a run of a test case; returns the run descriptor (with the run id
     * to poll via {@link #getCaseRun}). {@code opts} may be {@code null}.
     */
    public JsonNode runCase(String namespace, String caseId, Map<String, Object> opts) throws MockartyException {
        return client.post(casesBase(namespace) + "/" + enc(caseId) + "/run",
                opts == null ? Map.of() : opts, JsonNode.class);
    }

    /** List prior runs of a test case. */
    public List<JsonNode> listCaseRuns(String namespace, String caseId) throws MockartyException {
        JsonNode data = client.get(casesBase(namespace) + "/" + enc(caseId) + "/runs", JsonNode.class);
        return array(data, "runs", "caseRuns", "case_runs", "items");
    }

    // -- case-runs --

    /** Fetch a single case-run by id. */
    public JsonNode getCaseRun(String namespace, String runId) throws MockartyException {
        return client.get(tcmBase(namespace) + "/case-runs/" + enc(runId), JsonNode.class);
    }

    /** Cancel an in-flight case-run. */
    public void cancelCaseRun(String namespace, String runId) throws MockartyException {
        client.post(tcmBase(namespace) + "/case-runs/" + enc(runId) + "/cancel", null);
    }

    // -- defects --

    /** File a defect (fields: {@code title}, {@code description}, {@code caseRunId}, …). */
    public JsonNode createDefect(String namespace, Map<String, Object> defect) throws MockartyException {
        return client.post(tcmBase(namespace) + "/defects", defect, JsonNode.class);
    }

    /** List defects, optionally filtered. */
    public List<JsonNode> listDefects(String namespace, Map<String, String> filters) throws MockartyException {
        JsonNode data = client.get(tcmBase(namespace) + "/defects" + query(filters), JsonNode.class);
        return array(data, "defects", "items");
    }

    /** Delete a defect by id. */
    public void deleteDefect(String namespace, String defectId) throws MockartyException {
        client.delete(tcmBase(namespace) + "/defects/" + enc(defectId));
    }

    // -- folders --

    /** Return the folder tree. */
    public List<JsonNode> getFolderTree(String namespace) throws MockartyException {
        return array(client.get(tcmBase(namespace) + "/folders/tree", JsonNode.class), "items");
    }

    /** List folders (flat). */
    public List<JsonNode> listFolders(String namespace) throws MockartyException {
        return array(client.get(tcmBase(namespace) + "/folders", JsonNode.class), "items");
    }

    /** Create a folder. */
    public JsonNode createFolder(String namespace, Map<String, Object> folder) throws MockartyException {
        return client.post(tcmBase(namespace) + "/folders", folder, JsonNode.class);
    }

    /** Update a folder. */
    public JsonNode updateFolder(String namespace, String folderId, Map<String, Object> fields) throws MockartyException {
        return client.patch(tcmBase(namespace) + "/folders/" + enc(folderId), fields, JsonNode.class);
    }

    /** Delete a folder. */
    public void deleteFolder(String namespace, String folderId) throws MockartyException {
        client.delete(tcmBase(namespace) + "/folders/" + enc(folderId));
    }

    /** Move a folder under a new parent. */
    public void moveFolder(String namespace, String folderId, String toParentId) throws MockartyException {
        client.post(tcmBase(namespace) + "/folders/" + enc(folderId) + "/move",
                Map.of("toParentId", toParentId));
    }

    // -- attachments --

    /**
     * Upload an attachment to a (parentKind, parentId) — e.g. a video,
     * screenshot, log or report attached to a case/run/step.
     */
    public JsonNode uploadAttachment(String namespace, String parentKind, String parentId,
                                     String filename, byte[] content, String mediaType) throws MockartyException {
        String path = tcmBase(namespace) + "/attachments/upload"
                + "?parentKind=" + enc(parentKind) + "&parentId=" + enc(parentId);
        String mt = (mediaType == null || mediaType.isEmpty()) ? "application/octet-stream" : mediaType;
        String boundary = "----MockartySdk" + Long.toHexString(System.identityHashCode(content));
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        try {
            String head = "--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                    + "Content-Type: " + mt + "\r\n\r\n";
            buf.write(head.getBytes(StandardCharsets.UTF_8));
            buf.write(content);
            buf.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new MockartyException("tcm: build multipart: " + e.getMessage(), e);
        }
        return client.postRaw(path, buf.toByteArray(),
                "multipart/form-data; boundary=" + boundary, JsonNode.class);
    }

    /** List attachments attached to a (parentKind, parentId). */
    public List<JsonNode> listAttachments(String namespace, String parentKind, String parentId) throws MockartyException {
        String path = tcmBase(namespace) + "/attachments?parentKind=" + enc(parentKind) + "&parentId=" + enc(parentId);
        return array(client.get(path, JsonNode.class), "items", "attachments");
    }

    /** Download an attachment's raw bytes. */
    public byte[] downloadAttachment(String namespace, String attachmentId) throws MockartyException {
        return client.getBytes(tcmBase(namespace) + "/attachments/" + enc(attachmentId) + "/raw");
    }

    /** Delete an attachment by id. */
    public void deleteAttachment(String namespace, String attachmentId) throws MockartyException {
        client.delete(tcmBase(namespace) + "/attachments/" + enc(attachmentId));
    }

    private static List<JsonNode> array(JsonNode data, String... keys) {
        List<JsonNode> out = new ArrayList<>();
        if (data == null) {
            return out;
        }
        if (data.isArray()) {
            for (JsonNode n : data) {
                out.add(n);
            }
            return out;
        }
        for (String k : keys) {
            if (data.path(k).isArray()) {
                for (JsonNode n : data.path(k)) {
                    out.add(n);
                }
                return out;
            }
        }
        return out;
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String query(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new java.util.TreeMap<>(params).entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }
            sb.append(sb.length() == 0 ? "?" : "&")
                    .append(enc(e.getKey())).append("=").append(enc(e.getValue()));
        }
        return sb.toString();
    }
}
