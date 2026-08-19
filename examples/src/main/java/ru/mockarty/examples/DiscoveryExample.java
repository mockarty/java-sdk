// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryManifestCase;
import ru.mockarty.model.DiscoveryResult;

import java.util.List;

/**
 * Test-discovery sync example.
 *
 * <p>Discovery uploads a manifest of the FULL test inventory a framework
 * adapter knows about (including tests that did not run) so the Mockarty
 * TCM catalogue mirrors the source tree. New tests are created, existing
 * tests keep their human-authored metadata, and tests absent from an
 * authoritative manifest are marked orphaned (never deleted). This
 * complements {@code client.externalRuns()}, which ships per-test
 * RESULTS.</p>
 *
 * <h2>Two ways to sync</h2>
 *
 * <p><b>1. Automatic, from a JUnit 5 run.</b> The {@code mockarty-junit5}
 * module ships a JUnit Platform {@code TestExecutionListener}
 * ({@code ru.mockarty.junit5.discovery.MockartyDiscoveryListener}) that is
 * auto-registered via SPI. It is OFF by default; enable it for a CI
 * collect-and-sync step with a system property (or the matching env
 * var):</p>
 * <pre>{@code
 * ./gradlew test \
 *   -Dmockarty.discover=true \
 *   -Dmockarty.discover.source=junit5:auth-suite \
 *   -DMOCKARTY_BASE_URL=https://mockarty.example.com \
 *   -DMOCKARTY_API_KEY=mk_... \
 *   -DMOCKARTY_NAMESPACE=qa
 * }</pre>
 * <p>The listener walks the discovered test plan, builds a manifest
 * (fullName = {@code Class#method}, name = display name, suite = the test
 * class, sourceRef = {@code File.java}, labels = JUnit {@code @Tag}s) and
 * POSTs it once before the suite runs. Errors are swallowed so a sync
 * never fails the build.</p>
 *
 * <p><b>2. Manual, via the client.</b> Build the manifest yourself — handy
 * when you enumerate tests from a non-JUnit source (a generated test
 * matrix, a spec file, another harness). That is what this program
 * demonstrates.</p>
 */
public class DiscoveryExample {

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("MOCKARTY_BASE_URL", "http://localhost:5770");
        String apiKey = System.getenv().getOrDefault("MOCKARTY_API_KEY", "your-api-key");
        String namespace = System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "qa");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .namespace(namespace)
                .build()) {

            // The scope key. Pruning is scoped to it, so each suite owns its
            // own slice of the catalogue and never orphans another's cases.
            DiscoveryManifest manifest = new DiscoveryManifest("junit5:auth-suite")
                    .framework("junit5")
                    // pruneMissing=true → tests previously discovered under this
                    // source but absent from THIS manifest are marked orphaned
                    // (e.g. someone deleted a test in code). Set false for an
                    // additive-only sync.
                    .pruneMissing(true)
                    .addCase(new DiscoveryManifestCase(
                            "com.example.AuthTest#testLogin", "testLogin")
                            .suite("AuthTest")
                            .sourceRef("AuthTest.java")
                            .description("Happy-path login")
                            .labels(List.of("smoke", "auth")))
                    .addCase(new DiscoveryManifestCase(
                            "com.example.AuthTest#testLogout", "testLogout")
                            .suite("AuthTest")
                            .sourceRef("AuthTest.java")
                            .labels(List.of("auth")))
                    .addCase(new DiscoveryManifestCase(
                            "com.example.AuthTest#testRefreshToken", "testRefreshToken")
                            .suite("AuthTest")
                            .sourceRef("AuthTest.java"));

            DiscoveryResult result = client.discovery().sync(namespace, manifest);

            System.out.println("Discovery synced for source: " + result.getSource());
            System.out.println("  created:  " + result.getCreated());
            System.out.println("  updated:  " + result.getUpdated());
            System.out.println("  orphaned: " + result.getOrphaned());
            System.out.println("  total:    " + result.getTotal());
        }
    }
}
