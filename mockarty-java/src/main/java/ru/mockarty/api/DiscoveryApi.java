// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryManifestCase;
import ru.mockarty.model.DiscoveryResult;

/**
 * Client for {@code POST /api/v1/namespaces/:namespace/tcm/discovery}.
 *
 * <p>Where {@link ExternalRunsApi} ships per-test RESULTS, this endpoint
 * syncs a manifest of the FULL test inventory an SDK/CI adapter knows
 * about (including tests that did not run), so the TCM catalogue mirrors
 * the code base. New tests are created; existing tests keep their
 * human-authored metadata; tests absent from an authoritative manifest are
 * marked orphaned (never deleted).</p>
 *
 * <p>Identity is the per-case {@code fullName} — the same key a later
 * {@code /tcm/external-runs} report uses, so a test discovered here and
 * later executed lands on the same case.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * DiscoveryResult res = client.discovery().sync("qa",
 *     new DiscoveryManifest("junit5:auth-suite")
 *         .framework("junit5")
 *         .pruneMissing(true)
 *         .addCase(new DiscoveryManifestCase(
 *                 "com.example.AuthTest#testLogin", "testLogin")
 *             .suite("AuthTest")
 *             .sourceRef("AuthTest.java")
 *             .labels(java.util.List.of("smoke"))));
 * System.out.println("created=" + res.getCreated()
 *     + " orphaned=" + res.getOrphaned());
 * }</pre>
 *
 * <p>Errors: 400 on an invalid manifest (missing source / a case with no
 * fullName), 401/403 on auth/licence, 429 when the server is saturated by
 * concurrent syncs (retry with backoff).</p>
 */
public class DiscoveryApi {

    private final MockartyClient client;

    public DiscoveryApi(MockartyClient client) {
        this.client = client;
    }

    /**
     * Sync a discovery manifest, upserting cases by {@code fullName} and
     * (when {@code pruneMissing} is set) marking absent cases as orphaned.
     *
     * @param namespace target namespace; required.
     * @param manifest  the manifest; must carry a non-empty {@code source}
     *                  and a non-empty {@code fullName} on every case.
     * @return the per-sync summary (created / updated / orphaned / total).
     */
    public DiscoveryResult sync(String namespace, DiscoveryManifest manifest)
            throws MockartyException {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        if (manifest == null) {
            throw new IllegalArgumentException("manifest is required");
        }
        if (manifest.getSource() == null || manifest.getSource().isEmpty()) {
            throw new IllegalArgumentException("manifest.source is required");
        }
        if (manifest.getCases() != null) {
            for (DiscoveryManifestCase c : manifest.getCases()) {
                if (c == null || c.getFullName() == null || c.getFullName().isEmpty()) {
                    throw new IllegalArgumentException(
                            "every manifest case requires a non-empty fullName");
                }
            }
        }
        String path = "/api/v1/namespaces/" + namespace + "/tcm/discovery";
        return client.post(path, manifest, DiscoveryResult.class);
    }
}
