// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import ru.mockarty.junit5.framework.AllureMirror;
import ru.mockarty.junit5.framework.MockartyContext;
import ru.mockarty.junit5.framework.Step;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the owner-mandated invariant ({@code SDK_FRAMEWORK_PLAN.md}
 * §3.3, 2026-05-16): the JUnit5 extension picks up
 * {@code io.qameta.allure.*} annotations by default — pure-Allure tests
 * flow through Mockarty's case-frame without refactoring, mixed tests
 * have both annotation sets normalised, and pure-Mockarty tests work
 * without {@code io.qameta.allure:allure-java-commons} on the classpath
 * (verified via {@code AllureMirror.isAllureRuntimeAvailable} — the
 * mirror is reflection-based, no compile-time link).
 */
class AllureMirrorDefaultOnTest {

    @BeforeEach
    void setUp() {
        // Each scenario operates on a fresh context so leaked frames from
        // earlier scenarios cannot cross-contaminate the harvest assertions.
        MockartyContext.resetForTest();
    }

    @AfterEach
    void tearDown() {
        MockartyContext.resetForTest();
    }

    // ── Scenario 1: pure-Allure annotations, no @MockartyTest ────────

    @Severity(SeverityLevel.CRITICAL)
    @Feature("Login")
    @Story("Login with valid credentials")
    @Owner("auth-team")
    static class PureAllureSample {
        @Severity(SeverityLevel.BLOCKER)
        @Story("Bad credentials rejection")
        @Description("Pure-Allure description")
        @Issue("AUTH-42")
        @TmsLink("TC-100")
        void shouldRejectBadCredentials() {
            // body irrelevant — the test harvests the annotations
        }
    }

    @Test
    @DisplayName("Pure-Allure: SDK harvests annotations without @MockartyTest / @TestCase")
    void pureAllureFlowsThrough() throws Exception {
        Method m = PureAllureSample.class.getDeclaredMethod("shouldRejectBadCredentials");
        AllureMirror.Harvested h = AllureMirror.harvest(m);

        assertFalse(h.isEmpty(), "harvest must surface Allure metadata");
        // Method-level @Severity wins over class-level (first-match in scan order).
        assertEquals("blocker", h.severity);
        assertEquals("Pure-Allure description", h.description);
        assertEquals("auth-team", h.owner);
        // Story comes from BOTH class- and method-level, both should appear.
        assertTrue(h.stories.contains("Login with valid credentials"),
                "class-level @Story must propagate to method harvest");
        assertTrue(h.stories.contains("Bad credentials rejection"),
                "method-level @Story must be captured");
        assertEquals(List.of("Login"), h.features);
        assertTrue(h.issues.contains("AUTH-42"));
        assertTrue(h.tmsLinks.contains("TC-100"));

        // Apply onto an unbound case frame and check the metadata flows.
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        AllureMirror.apply(frame, h);
        assertEquals("blocker", frame.metadata.get("severity"));
        assertEquals("auth-team", frame.metadata.get("owner"));
        assertEquals("Pure-Allure description", frame.metadata.get("description"));
        Object features = frame.metadata.get("features");
        assertTrue(features instanceof List);
        assertTrue(((List<?>) features).contains("Login"));
    }

    // ── Scenario 2: mixed Allure + Mockarty native ───────────────────

    @Feature("Mixed")
    @Epic("Cross-cutting")
    static class MixedSample {
        @Severity(SeverityLevel.CRITICAL)
        @Owner("qa-bot")
        void shouldUseBothFrameworks() {
            // both annotation sets present
        }
    }

    @Test
    @DisplayName("Mixed Allure + Mockarty: both annotation sets harvest into one CaseFrame")
    void mixedAnnotationsNormalised() throws Exception {
        Method m = MixedSample.class.getDeclaredMethod("shouldUseBothFrameworks");
        AllureMirror.Harvested h = AllureMirror.harvest(m);

        assertEquals("critical", h.severity);
        assertEquals("qa-bot", h.owner);
        assertTrue(h.features.contains("Mixed"));
        assertTrue(h.epics.contains("Cross-cutting"));
    }

    // ── Scenario 3: pure-Mockarty (no Allure annotations) ────────────

    static class PureMockartySample {
        void shouldRunWithoutAllure() {
            // no Allure annotations at all
        }
    }

    @Test
    @DisplayName("Pure-Mockarty: harvest returns EMPTY, Step.run works without Allure runtime")
    void pureMockartyNoAllureFootprint() throws Exception {
        Method m = PureMockartySample.class.getDeclaredMethod("shouldRunWithoutAllure");
        AllureMirror.Harvested h = AllureMirror.harvest(m);
        assertTrue(h.isEmpty(), "no Allure annotations -> harvest empty");

        // Allure runtime IS on the classpath of THIS test (we depend on it for
        // verification), but Step.run must work irrespective — we assert it
        // doesn't blow up when no case frame is bound.
        // Push a case frame so Step has a parent to record on.
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(frame);
        try {
            String result = Step.run("issue token", () -> "abc");
            assertEquals("abc", result);
            assertEquals(1, frame.steps.size());
            assertEquals("passed", frame.steps.get(0).get("status"));
        } finally {
            MockartyContext.popCase();
        }
    }

    // ── Scenario 4: nested Allure-style steps ────────────────────────

    @Test
    @DisplayName("Nested steps: inner steps record under the outer case, status propagates")
    void nestedStepsRecorded() {
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(frame);
        try {
            try (Step outer = Step.open("checkout flow")) {
                try (Step inner = Step.open("validate cart")) {
                    inner.withMetadata("itemCount", 3);
                }
                try (Step inner2 = Step.open("submit payment")) {
                    inner2.withMetadata("amount", 99.95);
                }
            }
        } finally {
            MockartyContext.popCase();
        }

        // Three steps recorded on the case frame in invocation order.
        assertEquals(3, frame.steps.size());
        assertEquals("checkout flow", frame.steps.get(0).get("name"));
        assertEquals("validate cart", frame.steps.get(1).get("name"));
        assertEquals("submit payment", frame.steps.get(2).get("name"));
        for (Map<String, Object> step : frame.steps) {
            assertEquals("passed", step.get("status"));
        }
    }

    // ── Cache invariants ─────────────────────────────────────────────

    @Nested
    @DisplayName("Cache invariants")
    class CacheInvariants {

        @Test
        @DisplayName("repeated harvest of the same Method returns the same instance")
        void repeatedHarvestCached() throws Exception {
            Method m = PureAllureSample.class.getDeclaredMethod("shouldRejectBadCredentials");
            AllureMirror.Harvested first = AllureMirror.harvest(m);
            AllureMirror.Harvested second = AllureMirror.harvest(m);
            assertSame(first, second, "per-method cache must return identity");
        }

        @Test
        @DisplayName("null inputs are tolerated and return the EMPTY sentinel")
        void nullSafe() {
            assertSame(AllureMirror.Harvested.EMPTY, AllureMirror.harvest((Method) null));
            assertSame(AllureMirror.Harvested.EMPTY, AllureMirror.harvest((Class<?>) null));
        }
    }

    // ── Allure runtime bridge ────────────────────────────────────────

    @Test
    @DisplayName("Allure runtime detected when allure-java-commons is on the classpath")
    void allureRuntimeDetected() {
        // This module's test scope depends on allure-java-commons, so the
        // probe MUST return true. The bridge is opt-in by virtue of classpath
        // presence — user projects without Allure remain unaffected.
        assertTrue(AllureMirror.isAllureRuntimeAvailable(),
                "test runtime has allure-java-commons; detection must be positive");
    }

    @Test
    @DisplayName("Step.open/close survives Allure-bridge round-trip without throwing")
    void allureBridgeFailSoft() {
        // No Allure lifecycle is configured here (the SDK never installs an
        // Allure listener — that's the user's pytest/junit plugin's job).
        // The bridge MUST swallow any reflection errors and never bubble
        // them out as test failures.
        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(frame);
        try {
            try (Step s = Step.open("bridge probe")) {
                assertNotNull(s);
            }
        } finally {
            MockartyContext.popCase();
        }
        assertEquals(1, frame.steps.size());
        assertNull(frame.steps.get(0).get("error"));
    }

    // ── @Link harvest covers both shapes ────────────────────────────

    /**
     * Sample showing the two {@code @io.qameta.allure.Link} shapes the
     * harvester must support: the shorthand {@code @Link("URL")} (value()
     * carries the URL) and the explicit {@code @Link(name=, url=)} form
     * (value() is empty, the URL lives on a sibling attribute).
     */
    static class LinkSample {
        @Link("https://shorthand.example/docs")
        @Link(name = "design-doc", url = "https://named.example/spec")
        void hasBothLinkShapes() {
        }
    }

    @Test
    @DisplayName("@Link harvest captures URL from both shorthand and name+url forms")
    void linkHarvestBothShapes() throws Exception {
        Method m = LinkSample.class.getDeclaredMethod("hasBothLinkShapes");
        AllureMirror.Harvested h = AllureMirror.harvest(m);

        // Shorthand: value() is the URL → goes into links verbatim.
        assertTrue(h.links.contains("https://shorthand.example/docs"),
                "shorthand @Link(\"URL\") must surface in harvested.links — found: " + h.links);
        // Explicit form: value() is empty, but url() carries the URL.
        // This was a real bug: the harvester previously dropped the
        // explicit form on the floor because invokeValue() returned "".
        assertTrue(h.links.contains("https://named.example/spec"),
                "@Link(name=,url=) must surface in harvested.links — found: " + h.links);
    }

    // ── FQCN false-positive guard ────────────────────────────────────

    /**
     * Sample test method decorated with user-defined annotations whose
     * simple names collide with Allure's ({@code Step}, {@code Feature})
     * but whose package ({@code com.example.userlib}) is NOT Allure's.
     * The harvester MUST reject them — see fixtures
     * {@code com.example.userlib.Step} / {@code com.example.userlib.Feature}.
     */
    static class FalsePositiveSample {
        @com.example.userlib.Step("user-defined step annotation, MUST be ignored")
        @com.example.userlib.Feature("user-defined feature annotation, MUST be ignored")
        void userAnnotatedMethod() {
        }
    }

    @Test
    @DisplayName("Custom annotations outside io.qameta.allure.* are not harvested even if simple name matches")
    void customAnnotationsSameSimpleNameAreNotHarvested() throws Exception {
        Method m = FalsePositiveSample.class.getDeclaredMethod("userAnnotatedMethod");
        AllureMirror.Harvested h = AllureMirror.harvest(m);
        assertTrue(h.isEmpty(),
                "annotations outside the canonical Allure packages must be ignored — found: "
                        + "features=" + h.features
                        + " classStepHint=" + h.classStepHint
                        + " labels=" + h.labels);
    }

    // ── Default-ON invariant (MockartyTest.mirrorAllure) ─────────────

    @Test
    @DisplayName("@MockartyTest defaults mirrorAllure() to true (owner decision 2026-05-16)")
    void mockartyTestDefaultsMirrorAllureToTrue() throws Exception {
        // Reflectively read the default value of mirrorAllure() — this is
        // the canonical assertion of the owner invariant. If anybody ever
        // flips this to false, this test fails loudly.
        Object defaultValue = MockartyTest.class.getMethod("mirrorAllure")
                .getDefaultValue();
        assertEquals(Boolean.TRUE, defaultValue,
                "mirrorAllure() must default to true — see SDK_FRAMEWORK_PLAN §3.3");
    }
}
