// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;
import ru.mockarty.model.DiscoveryManifest;
import ru.mockarty.model.DiscoveryManifestCase;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link DiscoveryManifestAssembler} against a REAL JUnit
 * Platform {@link TestPlan} (discovered, not executed) built from the
 * {@link DiscoveryFixtures} fixture classes. This is the same
 * {@code TestPlan} the launcher hands to {@link MockartyDiscoveryListener}
 * at runtime, so the test validates the production tree-walk — not a mock.
 */
class DiscoveryManifestAssemblerTest {

    /** Discover (collect-only) the fixture class and assemble a manifest. */
    private static DiscoveryManifest assembleFor(Class<?> fixture, boolean prune) {
        Launcher launcher = LauncherFactory.create();
        LauncherDiscoveryRequest req = request()
                .selectors(selectClass(fixture))
                .build();
        TestPlan plan = launcher.discover(req);
        return DiscoveryManifestAssembler.assemble(plan, "junit5:unit", prune);
    }

    private static Map<String, DiscoveryManifestCase> byFullName(DiscoveryManifest m) {
        return m.getCases().stream()
                .collect(Collectors.toMap(DiscoveryManifestCase::getFullName, Function.identity()));
    }

    @Test
    @DisplayName("assembles one case per leaf test with junit5 framework + scope key")
    void assemblesLeafCases() {
        DiscoveryManifest m = assembleFor(DiscoveryFixtures.SampleTest.class, true);

        assertEquals("junit5:unit", m.getSource());
        assertEquals("junit5", m.getFramework());
        assertTrue(m.isPruneMissing());

        Map<String, DiscoveryManifestCase> byFull = byFullName(m);

        // Three @Test methods → three cases (containers are NOT emitted).
        assertEquals(3, m.getCases().size(), "only leaf @Test methods become cases");

        String cls = DiscoveryFixtures.SampleTest.class.getName();
        assertTrue(byFull.containsKey(cls + "#alpha"), "fullName is Class#method");
        assertTrue(byFull.containsKey(cls + "#beta"));
        assertTrue(byFull.containsKey(cls + "#gamma"));
    }

    @Test
    @DisplayName("name = display name, suite = test class, sourceRef = File.java")
    void mapsDisplayNameSuiteAndSourceRef() {
        DiscoveryManifest m = assembleFor(DiscoveryFixtures.SampleTest.class, false);
        Map<String, DiscoveryManifestCase> byFull = byFullName(m);

        DiscoveryManifestCase beta = byFull.get(DiscoveryFixtures.SampleTest.class.getName() + "#beta");
        assertNotNull(beta);
        // @DisplayName overrides the method name as the human name.
        assertEquals("Beta scenario", beta.getName());
        // suite is the enclosing test class's display name. For a real top-level
        // test class that's the simple class name; for these statically-nested
        // fixtures JUnit qualifies it as "DiscoveryFixtures$SampleTest", so we
        // assert the meaningful tail rather than couple to the fixture nesting.
        assertNotNull(beta.getSuite());
        assertTrue(beta.getSuite().endsWith("SampleTest"),
                "suite ends with the enclosing test class name, got: " + beta.getSuite());
        assertEquals("DiscoveryFixtures.java", beta.getSourceRef(),
                "sourceRef is the top-level source file (outer class), not the nested class");

        // pruneMissing=false must not be set.
        assertFalse(m.isPruneMissing());
    }

    @Test
    @DisplayName("@Tag values flow into case labels")
    void mapsTagsToLabels() {
        DiscoveryManifest m = assembleFor(DiscoveryFixtures.SampleTest.class, true);
        Map<String, DiscoveryManifestCase> byFull = byFullName(m);

        DiscoveryManifestCase alpha = byFull.get(DiscoveryFixtures.SampleTest.class.getName() + "#alpha");
        assertNotNull(alpha.getLabels());
        assertTrue(alpha.getLabels().contains("smoke"), "@Tag(\"smoke\") → label");
        assertTrue(alpha.getLabels().contains("fast"));

        DiscoveryManifestCase gamma = byFull.get(DiscoveryFixtures.SampleTest.class.getName() + "#gamma");
        // gamma has no @Tag — labels stay null (dropped from the wire).
        assertTrue(gamma.getLabels() == null || gamma.getLabels().isEmpty());
    }

    @Test
    @DisplayName("@Nested tests are descended into and emitted with the nested class as suite")
    void handlesNestedTests() {
        DiscoveryManifest m = assembleFor(DiscoveryFixtures.OuterTest.class, true);
        Map<String, DiscoveryManifestCase> byFull = byFullName(m);

        // One top-level test + one nested test.
        assertEquals(2, m.getCases().size());

        DiscoveryManifestCase nested = byFull.get(
                DiscoveryFixtures.OuterTest.Inner.class.getName() + "#innerCase");
        assertNotNull(nested, "the @Nested test must be discovered");
        assertNotNull(nested.getSuite());
        assertTrue(nested.getSuite().endsWith("Inner"),
                "nested test's suite is the nearest @Nested class, not the outer class, got: "
                        + nested.getSuite());
    }

    @Test
    @DisplayName("a null plan yields an empty manifest (fail-soft)")
    void nullPlanIsEmpty() {
        DiscoveryManifest m = DiscoveryManifestAssembler.assemble(null, "s", true);
        assertEquals("s", m.getSource());
        assertTrue(m.getCases().isEmpty());
    }
}
