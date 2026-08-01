// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behavioural tests for {@link MockartyDiscoveryListener}: the opt-in
 * switch, config resolution, and the end-to-end POST to {@code /tcm/discovery}
 * driven off a real discovered {@link TestPlan}.
 */
class MockartyDiscoveryListenerTest {

    private HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicReference<JsonNode> captured = new AtomicReference<>();
    private final AtomicReference<String> capturedNs = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        // Match any namespace path under /api/v1/namespaces/.../tcm/discovery.
        server.createContext("/api/v1/namespaces", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/tcm/discovery")) {
                hits.incrementAndGet();
                captured.set(mapper.readTree(exchange.getRequestBody()));
                // namespace is the path segment between /namespaces/ and /tcm
                String mid = path.substring("/api/v1/namespaces/".length());
                capturedNs.set(mid.substring(0, mid.indexOf("/tcm")));
                byte[] body = "{\"source\":\"junit5:probe\",\"created\":3,\"updated\":0,\"orphaned\":0,\"total\":3}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        // Leave no global config bleeding into sibling tests.
        System.clearProperty(MockartyDiscoveryListener.PROP_ENABLE);
        System.clearProperty(MockartyDiscoveryListener.PROP_SOURCE);
        System.clearProperty(MockartyDiscoveryListener.PROP_PRUNE);
        System.clearProperty("MOCKARTY_BASE_URL");
        System.clearProperty("MOCKARTY_NAMESPACE");
        System.clearProperty("MOCKARTY_API_KEY");
    }

    private TestPlan discover(Class<?> fixture) {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest req = request().selectors(selectClass(fixture)).build();
        return launcher.discover(req);
    }

    @Test
    @DisplayName("disabled by default: testPlanExecutionStarted does not hit the server")
    void disabledByDefault() {
        // No mockarty.discover property → listener stays dormant.
        new MockartyDiscoveryListener().testPlanExecutionStarted(
                discover(DiscoveryFixtures.SampleTest.class));
        assertEquals(0, hits.get(), "no sync must happen unless opted in");
    }

    @Test
    @DisplayName("enabled: POSTs the assembled manifest to /tcm/discovery in the configured namespace")
    void enabledPostsManifest() {
        System.setProperty(MockartyDiscoveryListener.PROP_ENABLE, "true");
        System.setProperty(MockartyDiscoveryListener.PROP_SOURCE, "junit5:probe");
        System.setProperty("MOCKARTY_BASE_URL", "http://localhost:" + server.getAddress().getPort());
        System.setProperty("MOCKARTY_NAMESPACE", "qa-disco");

        new MockartyDiscoveryListener().testPlanExecutionStarted(
                discover(DiscoveryFixtures.SampleTest.class));

        assertEquals(1, hits.get(), "exactly one sync for one test plan");
        assertEquals("qa-disco", capturedNs.get(), "namespace comes from MOCKARTY_NAMESPACE");

        JsonNode req = captured.get();
        assertNotNull(req);
        assertEquals("junit5:probe", req.get("source").asText());
        assertEquals("junit5", req.get("framework").asText());
        assertEquals(3, req.get("cases").size(), "all three fixture tests in the manifest");
    }

    @Test
    @DisplayName("enabled but connection refused: swallows the error (never fails the run)")
    void failSoftOnConnectionError() {
        System.setProperty(MockartyDiscoveryListener.PROP_ENABLE, "1");
        // Point at a closed port so the POST throws a connection error.
        server.stop(0);
        System.setProperty("MOCKARTY_BASE_URL", "http://localhost:" + server.getAddress().getPort());

        // Must not throw.
        new MockartyDiscoveryListener().testPlanExecutionStarted(
                discover(DiscoveryFixtures.SampleTest.class));
        assertEquals(0, hits.get());
    }

    @Test
    @DisplayName("config resolution: system property wins over env, parseBool aliases, defaults")
    void configResolution() {
        // enabled() is false with nothing set.
        assertFalse(MockartyDiscoveryListener.enabled());

        System.setProperty(MockartyDiscoveryListener.PROP_ENABLE, "yes");
        assertTrue(MockartyDiscoveryListener.enabled(), "'yes' is truthy");
        System.setProperty(MockartyDiscoveryListener.PROP_ENABLE, "off");
        assertFalse(MockartyDiscoveryListener.enabled(), "'off' is falsy");
        System.setProperty(MockartyDiscoveryListener.PROP_ENABLE, "garbage");
        assertFalse(MockartyDiscoveryListener.enabled(), "unknown → default (off)");

        // resolve(): sysprop takes precedence over the supplied default.
        System.setProperty(MockartyDiscoveryListener.PROP_SOURCE, "my-source");
        assertEquals("my-source",
                MockartyDiscoveryListener.resolve(MockartyDiscoveryListener.PROP_SOURCE,
                        MockartyDiscoveryListener.ENV_SOURCE, MockartyDiscoveryListener.DEFAULT_SOURCE));
        System.clearProperty(MockartyDiscoveryListener.PROP_SOURCE);
        assertEquals(MockartyDiscoveryListener.DEFAULT_SOURCE,
                MockartyDiscoveryListener.resolve(MockartyDiscoveryListener.PROP_SOURCE,
                        MockartyDiscoveryListener.ENV_SOURCE, MockartyDiscoveryListener.DEFAULT_SOURCE));

        // parseBool() truthy/falsy/unknown.
        assertTrue(MockartyDiscoveryListener.parseBool("TRUE", false));
        assertTrue(MockartyDiscoveryListener.parseBool("on", false));
        assertFalse(MockartyDiscoveryListener.parseBool("0", true));
        assertFalse(MockartyDiscoveryListener.parseBool("", true), "empty is explicitly falsy");
        assertTrue(MockartyDiscoveryListener.parseBool(null, true), "null → default");
        assertFalse(MockartyDiscoveryListener.parseBool("weird", false), "unknown → default");
    }
}
