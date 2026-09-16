// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.DelegatedCredential;
import ru.mockarty.model.DelegatedCredentialCreateRequest;
import ru.mockarty.model.DelegatedCredentialList;
import ru.mockarty.model.DelegatedCredentialWithToken;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Project-scoped delegated credentials (ledger item C-D-05).
 *
 * <p>An issuer — a Space owner, or one of their automation principals — freezes a
 * credential's namespace set, action set and expiry, and the holder cannot widen
 * any of the three. Use this instead of handing a contractor, a CI job or an MCP
 * client an API key, whose namespace is a default its holder can change.
 *
 * <p>The three properties are enforced by the server, not by this client: the
 * scope is read from the credential's stored row on every request, a namespace
 * header or query parameter cannot widen it, and the row itself rejects any
 * write to the scope columns. This client therefore validates only what a
 * round-trip cannot fix, and never NARROWS a requested scope — a request that
 * would exceed the caller's own authority is the server's refusal to make, and
 * silently shrinking it here would hand the caller a credential that does not do
 * what it asked for, with the escalation attempt invisible in the audit trail.
 */
public final class DelegatedCredentialsApi {
    /** Marks a delegated-credential bearer; distinct from mk_ and mki_ prefixes. */
    public static final String TOKEN_PREFIX = "mkd_";

    /** The closed set of actions a delegated scope may name. */
    public static final List<String> ACTION_VOCABULARY = List.of("read", "write", "delete");

    /** Mirror of the server's cap on how far ahead an expiry may be pinned. */
    public static final Duration MAX_TTL = Duration.ofDays(365);

    private static final String BASE_PATH = "/api/v1/auth/delegated-credentials";

    private final MockartyClient client;

    public DelegatedCredentialsApi(MockartyClient client) {
        if (client == null) throw new IllegalArgumentException("client is required");
        this.client = client;
    }

    /** Issues a delegated credential and returns its plain bearer exactly once. */
    public DelegatedCredentialWithToken create(DelegatedCredentialCreateRequest request)
            throws MockartyException {
        if (request == null) throw new IllegalArgumentException("request is required");
        validate(request);
        return client.post(BASE_PATH, wireBody(request), DelegatedCredentialWithToken.class);
    }

    /**
     * Lists the credentials THIS caller may manage.
     *
     * <p>A user session sees the credentials it issued; a delegated credential sees
     * only the ones it minted itself, never its issuer's.
     */
    public DelegatedCredentialList list() throws MockartyException {
        return client.get(BASE_PATH, DelegatedCredentialList.class);
    }

    /**
     * Reads one credential.
     *
     * <p>A credential outside the caller's authority is reported as not-found rather
     * than forbidden, so a delegated credential cannot enumerate its issuer's
     * credentials by probing ids.
     */
    public DelegatedCredential get(String credentialId) throws MockartyException {
        return client.get(credentialPath(credentialId), DelegatedCredential.class);
    }

    /**
     * Permanently disables a credential.
     *
     * <p>Idempotent: revoking an already revoked credential is a no-op rather than
     * an error, because the terminal state the caller wanted is the state that
     * holds.
     */
    public void revoke(String credentialId) throws MockartyException {
        client.delete(credentialPath(credentialId));
    }

    /**
     * Issues a fresh bearer, leaving the frozen scope untouched.
     *
     * <p>The operation to reach for when a secret leaked but the work must
     * continue. Rotation is deliberately NOT a scope change: changing a live
     * credential's authority means re-issuing, so the change is a new credential
     * with its own audit trail rather than a silent edit.
     */
    public DelegatedCredentialWithToken rotate(String credentialId) throws MockartyException {
        return client.post(credentialPath(credentialId) + "/rotate", null,
                DelegatedCredentialWithToken.class);
    }

    /**
     * Mirrors the server's scope rules so a caller learns about a malformed request
     * without a round-trip. It deliberately never narrows anything.
     */
    private static void validate(DelegatedCredentialCreateRequest request) {
        List<String> namespaces = new ArrayList<>();
        for (String namespace : request.getNamespaces()) {
            if (namespace == null || namespace.isBlank()) {
                throw new IllegalArgumentException("namespace entries must not be blank");
            }
            namespaces.add(namespace);
        }
        if (namespaces.isEmpty()) {
            throw new IllegalArgumentException(
                    "at least one namespace is required — an unscoped delegated credential is an API key");
        }
        Instant expiresAt = request.getExpiresAt();
        if (expiresAt == null) {
            throw new IllegalArgumentException(
                    "expiresAt is required — an unbounded delegated credential is an API key");
        }
        Instant now = Instant.now();
        if (!expiresAt.isAfter(now)) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }
        if (Duration.between(now, expiresAt).compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("expiresAt may be at most " + MAX_TTL.toDays() + " days ahead");
        }
        for (String action : request.getAllowedActions()) {
            if (!ACTION_VOCABULARY.contains(action)) {
                throw new IllegalArgumentException("'" + action + "' is not a known action (want one of "
                        + String.join(", ", ACTION_VOCABULARY) + ")");
            }
        }
    }

    /**
     * Renders the wire body. The expiry is formatted here rather than left to
     * Jackson: the client's ObjectMapper registers an Instant DESERIALIZER but no
     * serializer, so an Instant field would reach the server as a numeric
     * timestamp the server cannot bind.
     */
    private static Map<String, Object> wireBody(DelegatedCredentialCreateRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", request.getName());
        body.put("namespaces", request.getNamespaces());
        body.put("expires_at", request.getExpiresAt().toString());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            body.put("description", request.getDescription());
        }
        if (!request.getAllowedActions().isEmpty()) {
            body.put("allowed_actions", request.getAllowedActions());
        }
        if (request.getMetadata() != null) {
            body.put("metadata", request.getMetadata());
        }
        return body;
    }

    private static String credentialPath(String credentialId) {
        if (credentialId == null || credentialId.isBlank()) {
            throw new IllegalArgumentException("a delegated credential id is required");
        }
        return BASE_PATH + "/" + encode(credentialId);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
