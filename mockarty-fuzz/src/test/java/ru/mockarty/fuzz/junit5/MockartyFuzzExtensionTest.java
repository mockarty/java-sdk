// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import ru.mockarty.fuzz.Mutator;
import ru.mockarty.fuzz.Runner;
import ru.mockarty.fuzz.Seed;
import ru.mockarty.fuzz.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the @MockartyFuzz / @FuzzBuilder auto-wired flow end-to-end
 * via the JUnit Platform launcher API. Running the inner test classes
 * directly via @Nested would be simpler — but @Nested doesn't honour
 * @MockartyFuzz on the inner class (parent context wins), so we run
 * them as standalone classes through the platform.
 */
class MockartyFuzzExtensionTest {

    @Test
    void happyPathInjectsTargetAndRunner() {
        TestExecutionSummary summary = run(GoodFixture.class);
        assertEquals(1, summary.getTestsFoundCount(), "found one test");
        assertEquals(1, summary.getTestsSucceededCount(),
                () -> "failures: " + summary.getFailures());
    }

    @Test
    void missingBuilderFailsFast() {
        TestExecutionSummary summary = run(NoBuilderFixture.class);
        assertEquals(1, summary.getTestsFailedCount(), "failure expected");
        String trace = summary.getFailures().get(0).getException().toString();
        assertTrue(trace.contains("@FuzzBuilder"),
                "exception should mention @FuzzBuilder: " + trace);
    }

    @Test
    void nonStaticBuilderRejected() {
        TestExecutionSummary summary = run(NonStaticBuilderFixture.class);
        assertEquals(1, summary.getTestsFailedCount());
        assertTrue(summary.getFailures().get(0).getException().toString().contains("must be static"));
    }

    @Test
    void wrongReturnTypeRejected() {
        TestExecutionSummary summary = run(WrongReturnTypeFixture.class);
        assertEquals(1, summary.getTestsFailedCount());
        assertTrue(summary.getFailures().get(0).getException().toString().contains("must return Target"));
    }

    private TestExecutionSummary run(Class<?> testClass) {
        LauncherDiscoveryRequest req = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(req);
        return listener.getSummary();
    }

    // ── Inner fixtures (each is a real, separately-run JUnit class) ──

    @ExtendWith(MockartyFuzzExtension.class)
    @MockartyFuzz(adminUrl = "http://example.invalid", namespace = "ns", apiToken = "tok")
    static class GoodFixture {
        @FuzzBuilder
        static Target build() {
            return Target.named("ext-test")
                    .httpEndpoint("POST", "/x")
                    .seeds(Seed.of("s", "{}"))
                    .mutator(Mutator.JSON)
                    .build();
        }

        @Test
        void injectionWorks(Target target, Runner runner) {
            assertNotNull(target);
            assertNotNull(runner);
            assertEquals("ext-test", target.name());
        }
    }

    @ExtendWith(MockartyFuzzExtension.class)
    @MockartyFuzz
    static class NoBuilderFixture {
        @Test
        void shouldFail(Target target) {
            // never reached — beforeEach throws on missing @FuzzBuilder
        }
    }

    @ExtendWith(MockartyFuzzExtension.class)
    @MockartyFuzz
    static class NonStaticBuilderFixture {
        @FuzzBuilder
        Target buildInstance() {
            return Target.named("x").httpEndpoint("GET", "/y")
                    .seeds(Seed.of("s", "{}")).mutator(Mutator.JSON).build();
        }

        @Test
        void shouldFail() {}
    }

    @ExtendWith(MockartyFuzzExtension.class)
    @MockartyFuzz
    static class WrongReturnTypeFixture {
        @FuzzBuilder
        static String buildWrong() { return "nope"; }

        @Test
        void shouldFail() {}
    }
}
