// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.DelegatedCredential;
import ru.mockarty.model.DelegatedCredentialCreateRequest;
import ru.mockarty.model.DelegatedCredentialWithToken;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Issue a project-scoped delegated credential, hand it to a CI job, rotate and revoke it.
 *
 * <p>Run against a live Mockarty admin node:
 * <pre>
 * MOCKARTY_URL=http://127.0.0.1:5770 MOCKARTY_API_TOKEN=mk_... \
 *   ./gradlew :examples:run --args=''   # or invoke main() from an IDE
 * </pre>
 *
 * <p>The point of the example is the difference from an API key: an API key's
 * namespace is a DEFAULT its holder can change, and it carries no expiry of its
 * own. A delegated credential freezes both, and the holder — the CI job in this
 * story — has no path to widen either.
 */
public final class DelegatedCredentialsExample {
    private DelegatedCredentialsExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_URL", "http://127.0.0.1:5770"))
                .apiKey(System.getenv("MOCKARTY_API_TOKEN"))
                .build()) {

            // The ISSUER is a human (or a Space owner's automation): a session or an
            // API key. The credential it mints acts in the issuer's namespaces but only
            // within the frozen set, and only until the frozen instant.
            DelegatedCredentialWithToken issued = client.delegatedCredentials().create(
                    DelegatedCredentialCreateRequest.builder()
                            .name("ci-reader")
                            .description("nightly regression job — read-only, one namespace, 30 days")
                            .namespaces(List.of("staging"))
                            // Narrow explicitly. Omit to grant the full vocabulary.
                            .allowedActions(List.of("read"))
                            .expiresAt(Instant.now().plus(Duration.ofDays(30)))
                            .build());

            // Shown ONCE. Store it in the CI secret store: the row keeps only a bcrypt
            // hash and a sha256 index, so the bearer cannot be read back. Lost it? Rotate.
            System.out.println("issued " + issued.getCredential().getId()
                    + " (" + issued.getCredential().getTokenPrefix() + ")");
            System.out.println("  frozen scope: namespaces=" + issued.getCredential().getNamespaces()
                    + " actions=" + issued.getCredential().getAllowedActions()
                    + " expires=" + issued.getCredential().getExpiresAt());
            System.out.println("  hand this to the job: " + issued.getToken());
            System.out.println("  the full action vocabulary is: "
                    + String.join(", ", ru.mockarty.api.DelegatedCredentialsApi.ACTION_VOCABULARY));

            // The issuer can see what it issued — and so can a delegated credential, for
            // the credentials IT minted, but never for its issuer's.
            System.out.println();
            System.out.println(client.delegatedCredentials().list().getCount()
                    + " credential(s) visible to this caller");
            for (DelegatedCredential credential : client.delegatedCredentials().list().getCredentials()) {
                String state = credential.isRevoked() ? "revoked"
                        : (credential.isExpired(Instant.now()) ? "expired" : "active");
                System.out.println("  " + credential.getId() + " " + credential.getName()
                        + " " + state + " " + credential.getNamespaces()
                        + " -> " + credential.getExpiresAt());
            }

            // The leaked-secret path. Rotation issues a fresh bearer and leaves the scope
            // EXACTLY as it was: a live credential's authority is never edited, because a
            // silent widening would leave no new credential to audit.
            DelegatedCredentialWithToken rotated =
                    client.delegatedCredentials().rotate(issued.getCredential().getId());
            if (!rotated.getCredential().getNamespaces().equals(issued.getCredential().getNamespaces())) {
                throw new IllegalStateException("rotation must not change the scope");
            }
            System.out.println();
            System.out.println("rotated — the previous token stopped working immediately:");
            System.out.println("  " + rotated.getToken());

            // Revocation is terminal: there is no un-revoke, because "temporarily
            // revoked" is exactly the window a shared credential should not have.
            // Revoking twice is a no-op rather than an error.
            client.delegatedCredentials().revoke(issued.getCredential().getId());
            client.delegatedCredentials().revoke(issued.getCredential().getId());
            System.out.println();
            System.out.println("revoked " + issued.getCredential().getId() + " (idempotent)");

            System.out.println();
            System.out.println("the held token can act only in its frozen namespaces, "
                    + "only with its frozen actions, only until its expiry");
        }
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
