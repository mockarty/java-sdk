// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.MockartyForbiddenException;
import ru.mockarty.model.DelegatedCredential;
import ru.mockarty.model.DelegatedCredentialCreateRequest;
import ru.mockarty.model.DelegatedCredentialList;
import ru.mockarty.model.DelegatedCredentialWithToken;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The delegated-credential SDK contract.
 *
 * <p>The server payloads below are LITERAL JSON, key for key, rather than
 * something this test serialized itself: a test that round-trips the SDK's own
 * model agrees with itself no matter what the server sends, which is exactly how
 * a camelCase/snake_case mismatch shipped once already.
 */
class DelegatedCredentialsApiTest {
    private final List<Captured> requests = new ArrayList<>();
    private HttpServer server;
    private MockartyClient client;

    /** The response body the server actually emits, key for key. */
    private static final String CREDENTIAL_JSON = "{"
            + "\"created_at\":\"2026-09-16T12:00:00Z\","
            + "\"updated_at\":\"2026-09-16T12:00:00Z\","
            + "\"expires_at\":\"2026-09-17T12:00:00Z\","
            + "\"revoked_at\":\"2026-09-16T13:00:00Z\","
            + "\"last_used_at\":\"2026-09-16T12:30:00Z\","
            + "\"metadata\":{\"ticket\":\"C-D-05\"},"
            + "\"id\":\"cred-1\","
            + "\"name\":\"ci-reader\","
            + "\"description\":\"read-only CI credential\","
            + "\"issuer_user_id\":\"user-1\","
            + "\"issuer_credential_id\":\"cred-parent\","
            + "\"namespaces\":[\"alpha\",\"beta\"],"
            + "\"allowed_actions\":[\"read\"],"
            + "\"token_prefix\":\"mkd_abcd1234…\","
            + "\"issuer_user_role\":\"admin\","
            + "\"issuer_user_email\":\"owner@example.com\","
            + "\"issuer_user_login\":\"owner\"}";

    @BeforeEach void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/auth/delegated-credentials", this::handle);
        server.start();
        client = MockartyClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .apiKey("delegated-issuer-token").timeout(Duration.ofSeconds(5)).maxRetries(0).build();
    }

    @AfterEach void tearDown() { if (client != null) client.close(); if (server != null) server.stop(0); }

    /**
     * Every frozen field must survive the trip. Before the server's json tags were
     * restored this decoded a credential with no expiry and no issuer — the half of
     * the scope the credential is issued TO constrain.
     */
    @Test void responseDecodesTheFrozenScope() throws MockartyException {
        DelegatedCredential cred = client.delegatedCredentials().get("cred-1");

        assertEquals("cred-1", cred.getId());
        assertEquals("ci-reader", cred.getName());
        assertEquals("read-only CI credential", cred.getDescription());
        assertEquals("user-1", cred.getIssuerUserId());
        assertEquals("admin", cred.getIssuerUserRole());
        assertEquals("owner@example.com", cred.getIssuerUserEmail());
        assertEquals("owner", cred.getIssuerUserLogin());
        assertEquals("cred-parent", cred.getIssuerCredentialId());
        assertEquals("mkd_abcd1234…", cred.getTokenPrefix());
        assertEquals(List.of("alpha", "beta"), cred.getNamespaces());
        assertEquals(List.of("read"), cred.getAllowedActions());
        assertEquals("C-D-05", cred.getMetadata().get("ticket"));
        // Assert the timestamps decoded AT ALL first, or a missing expiry would
        // make the assertions below vacuously "correct".
        assertNotNull(cred.getExpiresAt());
        assertNotNull(cred.getCreatedAt());
        assertNotNull(cred.getUpdatedAt());
        assertNotNull(cred.getRevokedAt());
        assertNotNull(cred.getLastUsedAt());
        assertEquals(Instant.parse("2026-09-17T12:00:00Z"), cred.getExpiresAt());
    }

    @Test void scopeHelpersAnswerFromTheDecodedRow() throws MockartyException {
        DelegatedCredential cred = client.delegatedCredentials().get("cred-1");

        assertTrue(cred.hasNamespace("beta"));
        assertFalse(cred.hasNamespace("gamma"));
        assertFalse(cred.hasNamespace(null));
        assertTrue(cred.allows("read"));
        assertFalse(cred.allows("delete"));
        assertTrue(cred.isRevoked());
        assertFalse(cred.isExpired(Instant.parse("2026-09-16T13:00:00Z")));
        assertTrue(cred.isExpired(Instant.parse("2026-09-18T00:00:00Z")));
    }

    /** A row whose expiry did not decode must not read as unbounded. */
    @Test void aCredentialWithoutAnExpiryFailsClosed() {
        assertTrue(new DelegatedCredential().isExpired(Instant.parse("2026-09-16T12:00:00Z")));
    }

    @Test void lifecycleUsesExactPathsAndSnakeCaseBody() throws MockartyException {
        DelegatedCredentialWithToken issued = client.delegatedCredentials().create(
                DelegatedCredentialCreateRequest.builder()
                        .name("ci-reader")
                        .description("read-only CI credential")
                        .namespaces(List.of("alpha"))
                        .allowedActions(List.of("read"))
                        .expiresAt(Instant.now().plus(Duration.ofDays(30)))
                        .build());
        assertEquals("mkd_plainbearer", issued.getToken());
        assertEquals("cred-1", issued.getCredential().getId());

        DelegatedCredentialList page = client.delegatedCredentials().list();
        assertEquals(1, page.getCount());
        assertEquals("cred-1", page.getCredentials().get(0).getId());

        assertEquals("mkd_rotated", client.delegatedCredentials().rotate("cred-1").getToken());
        client.delegatedCredentials().revoke("cred-1");

        List<String> expected = List.of(
                "POST /api/v1/auth/delegated-credentials",
                "GET /api/v1/auth/delegated-credentials",
                "POST /api/v1/auth/delegated-credentials/cred-1/rotate",
                "DELETE /api/v1/auth/delegated-credentials/cred-1");
        assertEquals(expected.size(), requests.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), requests.get(i).methodPath);
        }

        String createBody = requests.get(0).body;
        assertTrue(createBody.contains("\"expires_at\""), "create body must carry expires_at: " + createBody);
        assertTrue(createBody.contains("\"namespaces\":[\"alpha\"]"), createBody);
        assertTrue(createBody.contains("\"allowed_actions\":[\"read\"]"), createBody);
        assertFalse(createBody.contains("expiresAt"), "create body leaked a camelCase expiresAt: " + createBody);
        assertFalse(createBody.contains("allowedActions"), createBody);
    }

    @Test void createOmitsOptionalKeysTheCallerDidNotSet() throws MockartyException {
        client.delegatedCredentials().create(DelegatedCredentialCreateRequest.builder()
                .namespaces(List.of("alpha"))
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build());
        String body = requests.get(0).body;
        assertFalse(body.contains("description"), body);
        assertFalse(body.contains("metadata"), body);
        // An omitted action set is the server's full vocabulary, not an empty grant.
        assertFalse(body.contains("allowed_actions"), body);
    }

    /**
     * A malformed scope must be refused without a round-trip: the server's own
     * refusals are 400/403 incidents an operator has to read, and spending one on
     * a request the client could have rejected locally hides the ones that matter.
     */
    @Test void aMalformedScopeIsRefusedBeforeNetwork() {
        Instant future = Instant.now().plus(Duration.ofHours(1));
        List<DelegatedCredentialCreateRequest> cases = List.of(
                DelegatedCredentialCreateRequest.builder().expiresAt(future).build(),
                DelegatedCredentialCreateRequest.builder().namespaces(List.of("  ")).expiresAt(future).build(),
                DelegatedCredentialCreateRequest.builder().namespaces(List.of("alpha")).build(),
                DelegatedCredentialCreateRequest.builder().namespaces(List.of("alpha"))
                        .expiresAt(Instant.now().minus(Duration.ofHours(1))).build(),
                DelegatedCredentialCreateRequest.builder().namespaces(List.of("alpha"))
                        .expiresAt(future.plus(DelegatedCredentialsApi.MAX_TTL)).build(),
                DelegatedCredentialCreateRequest.builder().namespaces(List.of("alpha"))
                        .allowedActions(List.of("admin")).expiresAt(future).build());

        for (DelegatedCredentialCreateRequest request : cases) {
            assertThrows(IllegalArgumentException.class, () -> client.delegatedCredentials().create(request));
        }
        assertEquals(0, requests.size(), "a locally-refused create must not reach the server");
    }

    @Test void anEmptyIdIsRefusedBeforeNetwork() {
        assertThrows(IllegalArgumentException.class, () -> client.delegatedCredentials().get("  "));
        assertThrows(IllegalArgumentException.class, () -> client.delegatedCredentials().rotate(""));
        assertThrows(IllegalArgumentException.class, () -> client.delegatedCredentials().revoke(null));
        assertEquals(0, requests.size());
    }

    /** A scope refusal is a 403, and must not be flattened into a generic failure. */
    @Test void aServerRefusalSurfacesAsAForbiddenApiError() {
        MockartyForbiddenException failure = assertThrows(MockartyForbiddenException.class,
                () -> client.delegatedCredentials().revoke("forbidden"));
        assertEquals(403, failure.getStatusCode());
        assertTrue(failure.getMessage().contains("scope exceeds"));
    }

    /**
     * The lifecycle is exactly five operations. A sixth public method would be a
     * surface the Go and Python clients do not have, and the three-language parity
     * check reads this class by reflection-free name extraction — so pinning it
     * here catches a divergence before the parity run does.
     */
    @Test void theApiExposesExactlyTheFiveLifecycleOperations() {
        List<String> operations = new ArrayList<>();
        for (java.lang.reflect.Method method : DelegatedCredentialsApi.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isPublic(method.getModifiers())
                    && Character.isLowerCase(method.getName().charAt(0))) {
                operations.add(method.getName());
            }
        }
        java.util.Collections.sort(operations);
        assertEquals(List.of("create", "get", "list", "revoke", "rotate"), operations);
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new Captured(exchange.getRequestMethod() + " " + exchange.getRequestURI(), body));

        String response;
        int status = 200;
        if (path.endsWith("/forbidden")) {
            status = 403;
            response = "{\"error\":\"requested scope exceeds this delegated credential's own scope\"}";
        } else if ("DELETE".equals(exchange.getRequestMethod())) {
            response = "{\"message\":\"Delegated credential revoked\"}";
        } else if (path.endsWith("/rotate")) {
            response = "{\"credential\":" + CREDENTIAL_JSON + ",\"token\":\"mkd_rotated\",\"message\":\"rotated\"}";
        } else if ("POST".equals(exchange.getRequestMethod())) {
            status = 201;
            response = "{\"credential\":" + CREDENTIAL_JSON + ",\"token\":\"mkd_plainbearer\",\"message\":\"created\"}";
        } else if (path.endsWith("/cred-1")) {
            response = CREDENTIAL_JSON;
        } else {
            response = "{\"credentials\":[" + CREDENTIAL_JSON + "],\"count\":1}";
        }

        byte[] payload = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private static final class Captured {
        private final String methodPath;
        private final String body;

        private Captured(String methodPath, String body) {
            this.methodPath = methodPath;
            this.body = body;
        }
    }
}
