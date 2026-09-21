// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Read an account as its owner, or a Team roster as an active admin. */
    public Map<String, Object> organization(String spaceId) throws MockartyException {
        spacePath(spaceId);
        return map(client.get("/api/v1/cloud/organization?space_id=" + encode(spaceId), Map.class));
    }

    /** Read published, expiring Team quotes as the owner of a Personal Space. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listTeamPackages(String personalSpaceId) throws MockartyException {
        spacePath(personalSpaceId);
        Map<String, Object> response = map(client.get(
                "/api/v1/cloud/team/packages?space_id=" + encode(personalSpaceId), Map.class));
        Object packages = response.get("packages");
        if (!(packages instanceof List<?>)) {
            throw new IllegalStateException("Team package quotes are missing");
        }
        return (List<Map<String, Object>>) packages;
    }

    /** Read Team billing as its current owner or billing manager. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTeamBilling(String teamSpaceId) throws MockartyException {
        spacePath(teamSpaceId);
        Map<String, Object> response = map(client.get(
                "/api/v1/cloud/team/billing?space_id=" + encode(teamSpaceId), Map.class));
        Object billing = response.get("billing");
        if (!(billing instanceof Map<?, ?>)) {
            throw new IllegalStateException("Team billing is missing");
        }
        return map((Map) billing);
    }

    /** Estimate the signed-in recipient's Personal Pro credit before accepting a Team invitation. */
    public Map<String, Object> previewTeamTransition(String token) throws MockartyException {
        Map<String, Object> response = map(client.get("/api/v1/cloud/team/invite-links/"
                + encode(require(token, "Team invite token")) + "/transition", Map.class));
        Object transition = response.get("transition");
        if (!(transition instanceof Map<?, ?>)) {
            throw new IllegalStateException("Team transition preview is missing");
        }
        return map((Map) transition);
    }

    /** Join one paid Team after reviewing the Personal Pro transition. Reuse the key on an uncertain retry. */
    public void acceptTeamInvite(String token, String idempotencyKey,
                                 boolean confirmTransition) throws MockartyException {
        if (!confirmTransition) {
            throw new IllegalArgumentException("Team transition must be explicitly confirmed");
        }
        client.postWithHeaders("/api/v1/cloud/team/invite-links/"
                + encode(require(token, "Team invite token")) + "/accept",
                Map.of(), Void.class, teamKey(idempotencyKey));
    }

    /** Read a redacted, newest-first Team and Space audit page as the seated Team owner.
     *  Pass next_before_id unchanged for the next page. Zero limit uses the server default of 50.
     */
    public Map<String, Object> listTeamAudit(String spaceId, String beforeId, int limit) throws MockartyException {
        spacePath(spaceId);
        if (limit < 0 || limit > 200) {
            throw new IllegalArgumentException("Team audit limit must be 1..200, or zero for the server default");
        }
        StringBuilder path = new StringBuilder("/api/v1/cloud/team/audit?space_id=").append(encode(spaceId));
        if (beforeId != null && !beforeId.isEmpty()) {
            try {
                if (Long.parseLong(beforeId) < 1) throw new NumberFormatException("nonpositive");
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("beforeId must be a positive decimal event id", e);
            }
            path.append("&before_id=").append(encode(beforeId));
        }
        if (limit > 0) path.append("&limit=").append(limit);
        return map(client.get(path.toString(), Map.class));
    }

    /** Add a Space under an active paid Team. Reuse the key for an uncertain retry. */
    public Map<String, Object> createTeamSpace(String teamId, String name, String audience,
                                                String slug, String idempotencyKey) throws MockartyException {
        if (!"team".equals(audience) && !"restricted".equals(audience)) {
            throw new IllegalArgumentException("audience must be team or restricted");
        }
        Map<String, String> body = new LinkedHashMap<>();
        body.put("team_id", require(teamId, "Team id"));
        body.put("name", require(name, "Space name"));
        body.put("audience", audience);
        if (slug != null && !slug.isBlank()) body.put("slug", slug);
        return map(client.postWithHeaders("/api/v1/cloud/team/spaces", body, Map.class,
                teamKey(idempotencyKey)));
    }

    /** Reserve a paid Seat and send an invitation; the token stays with the recipient. */
    public void inviteTeamMember(String spaceId, String email, String role,
                                 String idempotencyKey) throws MockartyException {
        spacePath(spaceId);
        if (!"member".equals(role) && !"admin".equals(role) && !"billing_manager".equals(role)) {
            throw new IllegalArgumentException("Team role must be member, admin or billing_manager");
        }
        client.postWithHeaders("/api/v1/cloud/team/invites",
                Map.of("space_id", spaceId, "email", require(email, "recipient email"), "role", role),
                Void.class, teamKey(idempotencyKey));
    }

    /** Release a pending Team Seat. Reuse the same key after an uncertain response. */
    public void revokeTeamInvite(String spaceId, String inviteId, String idempotencyKey) throws MockartyException {
        spacePath(spaceId);
        client.deleteWithHeaders("/api/v1/cloud/team/invites/" + encode(require(inviteId, "Team invite id"))
                + "?space_id=" + encode(spaceId),
                Map.of("Idempotency-Key", require(idempotencyKey, "idempotency key")));
    }

    /** Release a member's Team Seat and Team access while retaining their Personal Spaces. */
    public void removeTeamMember(String spaceId, String memberId, String idempotencyKey) throws MockartyException {
        spacePath(spaceId);
        client.deleteWithHeaders("/api/v1/cloud/team/members/" + encode(require(memberId, "Team member id"))
                + "?space_id=" + encode(spaceId), teamKey(idempotencyKey));
    }

    /** Suspend or resume a Team member while keeping their paid Seat assigned. */
    public void setTeamMemberStatus(String spaceId, String memberId, String status,
                                    String idempotencyKey) throws MockartyException {
        spacePath(spaceId);
        String normalized = require(status, "Team member status").trim().toLowerCase(java.util.Locale.ROOT);
        if (!"active".equals(normalized) && !"suspended".equals(normalized)) {
            throw new IllegalArgumentException("Team member status must be active or suspended");
        }
        client.patchWithHeaders("/api/v1/cloud/team/members/" + encode(require(memberId, "Team member id"))
                        + "/status?space_id=" + encode(spaceId), Map.of("status", normalized), Void.class,
                teamKey(idempotencyKey));
    }

    /** Appoint an active Team member without changing the payer or individual Space owners. */
    public void transferTeamOwner(String spaceId, String successorUserId, String idempotencyKey) throws MockartyException {
        spacePath(spaceId);
        client.postWithHeaders("/api/v1/cloud/team/owner-transfer",
                Map.of("space_id", spaceId, "successor_user_id", require(successorUserId, "successor user id")),
                Void.class, teamKey(idempotencyKey));
    }

    public Map<String, Object> rename(String spaceId, String name, String etag,
                                       String idempotencyKey) throws MockartyException {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Space name is required");
        return map(client.patchWithHeaders(spacePath(spaceId), Map.of("name", name), Map.class,
                mutationHeaders(etag, idempotencyKey)));
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

    private static Map<String, String> teamKey(String idempotencyKey) {
        return Map.of("Idempotency-Key", require(idempotencyKey, "idempotency key"));
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
