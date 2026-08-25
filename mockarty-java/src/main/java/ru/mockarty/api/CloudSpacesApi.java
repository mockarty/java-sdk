// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Curated collaboration API with explicit Space context on every scoped call. */
public class CloudSpacesApi {
    private final MockartyClient client;

    public CloudSpacesApi(MockartyClient client) { this.client = client; }

    public Map<String, Object> list(String cursor, int limit) throws MockartyException {
        return map(client.get("/api/v1/cloud/spaces" + pageQuery(cursor, limit), Map.class));
    }

    public Map<String, Object> get(String spaceId) throws MockartyException {
        return map(client.get(spacePath(spaceId), Map.class));
    }

    public Map<String, Object> listMembers(String spaceId, String cursor, int limit) throws MockartyException {
        return map(client.get(spacePath(spaceId) + "/members" + pageQuery(cursor, limit), Map.class));
    }

    public Map<String, Object> listInvites(String spaceId, String cursor, int limit) throws MockartyException {
        return map(client.get(spacePath(spaceId) + "/invites" + pageQuery(cursor, limit), Map.class));
    }

    public Map<String, Object> previewInvite(String token) throws MockartyException {
        return map(client.get("/api/v1/cloud/invites/" + encode(require(token, "invite token")), Map.class));
    }

    public Map<String, Object> createInvite(String spaceId, String email, String role, int expiresInHours,
                                             String etag, String idempotencyKey) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("role", role);
        if (expiresInHours > 0) body.put("expires_in_hours", expiresInHours);
        return map(client.postWithHeaders(spacePath(spaceId) + "/invites", body, Map.class,
                mutationHeaders(etag, idempotencyKey)));
    }

    public Map<String, Object> revokeInvite(String spaceId, String inviteId, String etag,
                                             String idempotencyKey) throws MockartyException {
        return map(client.deleteWithHeaders(spacePath(spaceId) + "/invites/" + encode(require(inviteId, "invite id")),
                Map.class, mutationHeaders(etag, idempotencyKey)));
    }

    public Map<String, Object> acceptInvite(String token, String etag, String idempotencyKey)
            throws MockartyException {
        return map(client.postWithHeaders("/api/v1/cloud/invites/" + encode(require(token, "invite token")) + "/accept",
                Map.of(), Map.class, mutationHeaders(etag, idempotencyKey)));
    }

    public Map<String, Object> updateMemberRole(String spaceId, String memberId, String role,
                                                 String etag, String idempotencyKey) throws MockartyException {
        return map(client.patchWithHeaders(spacePath(spaceId) + "/members/" + encode(require(memberId, "member id")),
                Map.of("role", role), Map.class, mutationHeaders(etag, idempotencyKey)));
    }

    public Map<String, Object> removeMember(String spaceId, String memberId, String etag,
                                             String idempotencyKey) throws MockartyException {
        return map(client.deleteWithHeaders(spacePath(spaceId) + "/members/" + encode(require(memberId, "member id")),
                Map.class, mutationHeaders(etag, idempotencyKey)));
    }

    private static String pageQuery(String cursor, int limit) {
        boolean hasCursor = cursor != null && !cursor.isBlank();
        if (!hasCursor && limit <= 0) return "";
        if (!hasCursor) return "?limit=" + limit;
        if (limit <= 0) return "?cursor=" + encode(cursor);
        return "?cursor=" + encode(cursor) + "&limit=" + limit;
    }

    private static String spacePath(String spaceId) {
        return "/api/v1/cloud/spaces/" + encode(require(spaceId, "Space id"));
    }

    private static Map<String, String> mutationHeaders(String etag, String idempotencyKey) {
        return Map.of("If-Match", require(etag, "Space ETag"),
                "Idempotency-Key", require(idempotencyKey, "idempotency key"));
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Map value) { return value == null ? Map.of() : value; }
}
