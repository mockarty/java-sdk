// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.discovery;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryResult;

/**
 * JUnit Platform {@link TestExecutionListener} that syncs the discovered
 * {@link TestPlan} to Mockarty TCM as a test-discovery manifest.
 *
 * <p>Where {@link ru.mockarty.junit5.MockartyExtension} ships per-test
 * RESULTS, this listener ships the full test INVENTORY (every test the
 * launcher collected — including ones that won't run in this invocation)
 * so the TCM catalogue mirrors the source tree. It fires once, at
 * {@link #testPlanExecutionStarted(TestPlan)}, before any test runs.</p>
 *
 * <h2>Opt-in</h2>
 * <p>Disabled by default — discovery is a CI/cataloguing concern, not
 * something every local {@code ./gradlew test} should hit the server with.
 * Enable by setting the system property {@code mockarty.discover=true} (or
 * the environment variable {@code MOCKARTY_DISCOVER=true}):</p>
 * <pre>{@code
 * ./gradlew test -Dmockarty.discover=true \
 *     -Dmockarty.discover.source=junit5:auth-suite
 * }</pre>
 *
 * <h2>Configuration</h2>
 * <p>Each key is read first as a system property, then as the matching
 * environment variable:</p>
 * <ul>
 *   <li>{@code mockarty.discover} / {@code MOCKARTY_DISCOVER} — master
 *       switch ({@code true}/{@code 1}/{@code yes}/{@code on}). Default
 *       off.</li>
 *   <li>{@code mockarty.discover.source} / {@code MOCKARTY_DISCOVER_SOURCE}
 *       — scope key. Default {@code junit5}. Pruning is scoped to it.</li>
 *   <li>{@code mockarty.discover.pruneMissing} /
 *       {@code MOCKARTY_DISCOVER_PRUNE} — orphan cases absent from this
 *       manifest. Default {@code true}.</li>
 *   <li>Server connection reuses the standard SDK config
 *       ({@code MOCKARTY_BASE_URL} / {@code MOCKARTY_API_KEY} /
 *       {@code MOCKARTY_NAMESPACE}), resolved by {@link MockartyClient}.</li>
 * </ul>
 *
 * <h2>Registration</h2>
 * <p>Auto-registered via the JUnit Platform {@code TestExecutionListener}
 * SPI ({@code META-INF/services/...} shipped in this module), so simply
 * having {@code mockarty-junit5} on the test classpath is enough — the
 * opt-in switch keeps it dormant until you ask for it. The existing
 * {@link ru.mockarty.junit5.MockartyExtension} result reporter is
 * unaffected; the two run side-by-side.</p>
 *
 * <h2>Fail-soft</h2>
 * <p>Any error (no manifest, connection refused, 4xx/5xx, 429) is logged
 * and swallowed — a discovery sync must never fail the test run.</p>
 */
public final class MockartyDiscoveryListener implements TestExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MockartyDiscoveryListener.class);

    static final String PROP_ENABLE = "mockarty.discover";
    static final String ENV_ENABLE = "MOCKARTY_DISCOVER";
    static final String PROP_SOURCE = "mockarty.discover.source";
    static final String ENV_SOURCE = "MOCKARTY_DISCOVER_SOURCE";
    static final String PROP_PRUNE = "mockarty.discover.pruneMissing";
    static final String ENV_PRUNE = "MOCKARTY_DISCOVER_PRUNE";

    static final String DEFAULT_SOURCE = "junit5";
    static final String DEFAULT_NAMESPACE = "sandbox";

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        if (!enabled()) {
            return;
        }
        try {
            String source = resolve(PROP_SOURCE, ENV_SOURCE, DEFAULT_SOURCE);
            boolean prune = parseBool(resolve(PROP_PRUNE, ENV_PRUNE, "true"), true);

            DiscoveryManifest manifest =
                    DiscoveryManifestAssembler.assemble(testPlan, source, prune);
            if (manifest.getCases().isEmpty()) {
                log.debug("Mockarty discovery: empty test plan, nothing to sync");
                return;
            }
            sync(manifest);
        } catch (Throwable t) {
            // Best-effort: never let cataloguing break the test run.
            log.warn("Mockarty discovery sync failed (ignored): {}", t.toString());
        }
    }

    private void sync(DiscoveryManifest manifest) {
        // Build the client from the standard env/sysprop config so the
        // listener needs no extra wiring. Namespace resolves the same way
        // (MOCKARTY_NAMESPACE) with a sandbox default.
        try (MockartyClient client = MockartyClient.create()) {
            String namespace = client.getConfig().getNamespace();
            if (namespace == null || namespace.isEmpty()) {
                namespace = DEFAULT_NAMESPACE;
            }
            DiscoveryResult result = client.discovery().syncDiscovery(namespace, manifest);
            log.info("Mockarty discovery synced: source={} created={} updated={} orphaned={} total={}",
                    result.getSource(), result.getCreated(), result.getUpdated(),
                    result.getOrphaned(), result.getTotal());
        }
    }

    // ── Config resolution ───────────────────────────────────────────

    /** Master switch — off unless explicitly enabled. */
    static boolean enabled() {
        String raw = resolve(PROP_ENABLE, ENV_ENABLE, null);
        return parseBool(raw, false);
    }

    /** System property first, then env var, then the supplied default. */
    static String resolve(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        v = System.getenv(env);
        if (v != null && !v.isEmpty()) {
            return v;
        }
        return def;
    }

    static boolean parseBool(String raw, boolean def) {
        if (raw == null) {
            return def;
        }
        switch (raw.trim().toLowerCase()) {
            case "true":
            case "1":
            case "yes":
            case "on":
                return true;
            case "false":
            case "0":
            case "no":
            case "off":
            case "":
                return false;
            default:
                return def;
        }
    }
}
