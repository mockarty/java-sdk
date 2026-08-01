// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * End-to-end proof that the Allure test plan restricts a real JUnit Platform
 * run: the fixture classes are discovered and EXECUTED through the launcher
 * with the filter installed, and the assertions are on which test methods
 * actually ran.
 */
class MockartyTestPlanFilterTest {

    private static AllureTestPlan plan(String json) {
        return AllureTestPlanLoader.parse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), "plan.json");
    }

    /**
     * Records the display names that actually executed — the assertion
     * surface for "only the listed tests ran".
     */
    private static final class RanCollector
            implements org.junit.platform.launcher.TestExecutionListener {

        static final RanCollector INSTANCE = new RanCollector();
        private final List<String> names = java.util.Collections.synchronizedList(new ArrayList<>());

        @Override
        public void executionStarted(org.junit.platform.launcher.TestIdentifier id) {
            if (id.isTest()) {
                names.add(methodName(id));
            }
        }

        private static String methodName(org.junit.platform.launcher.TestIdentifier id) {
            String display = id.getDisplayName();
            int paren = display.indexOf('(');
            return paren >= 0 ? display.substring(0, paren) : display;
        }

        List<String> drain() {
            synchronized (names) {
                List<String> copy = new ArrayList<>(names);
                names.clear();
                return copy;
            }
        }
    }

    /** Execute the fixtures and return the method names that ran. */
    private static List<String> run(MockartyTestPlanFilter filter) {
        RanCollector.INSTANCE.drain();
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(TestPlanFixtures.SelectorOnly.class),
                        selectClass(TestPlanFixtures.WithIds.class))
                .filters(filter)
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.execute(request, RanCollector.INSTANCE);
        return RanCollector.INSTANCE.drain().stream().sorted().collect(Collectors.toList());
    }

    @Test
    @DisplayName("regression guard: no plan configured runs everything")
    void noPlanRunsEverything() {
        List<String> ran = run(new MockartyTestPlanFilter((AllureTestPlan) null));
        assertEquals(Arrays.asList("alpha", "gamma", "pinnedByAnnotation",
                        "pinnedByMockartyCase", "pinnedByTag", "withParam"),
                ran, "without a plan the filter must be inert");
    }

    @Test
    @DisplayName("a selector runs only the listed test")
    void selectorRunsOnlyListedTest() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"selector\":\""
                + TestPlanFixtures.SelectorOnly.class.getName() + "#alpha\"}]}");
        assertEquals(java.util.Collections.singletonList("alpha"), run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("the dotted package.Class.method form from the Allure TestOps docs matches")
    void dottedSelectorMatches() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"selector\":\""
                + TestPlanFixtures.SelectorOnly.class.getName() + ".gamma\"}]}");
        assertEquals(java.util.Collections.singletonList("gamma"), run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("the fullName shape our discovery reports (Class#method(paramTypes)) matches")
    void fullNameWithParameterTypesMatches() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"selector\":\""
                + TestPlanFixtures.SelectorOnly.class.getName()
                + "#withParam(org.junit.jupiter.api.TestInfo)\"}]}");
        assertEquals(java.util.Collections.singletonList("withParam"), run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("a no-arg method is addressable with or without empty parens")
    void emptyParensSelectorMatches() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"selector\":\""
                + TestPlanFixtures.SelectorOnly.class.getName() + "#alpha()\"}]}");
        assertEquals(java.util.Collections.singletonList("alpha"), run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("id matches @AllureId read reflectively (no Allure on the classpath needed)")
    void idMatchesAllureIdAnnotation() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"id\":777}]}");
        assertEquals(java.util.Collections.singletonList("pinnedByAnnotation"),
                run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("id matches the @Tag(\"allure.id:...\") shape TestOps writes")
    void idMatchesAllureIdTag() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"id\":\"888\"}]}");
        assertEquals(java.util.Collections.singletonList("pinnedByTag"),
                run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("id matches @TestCase(\"CASE-9\") so Mockarty-native suites are addressable")
    void idMatchesMockartyTestCase() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"id\":\"CASE-9\"}]}");
        assertEquals(java.util.Collections.singletonList("pinnedByMockartyCase"),
                run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("several entries select several tests")
    void multipleEntries() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":["
                + "{\"selector\":\"" + TestPlanFixtures.SelectorOnly.class.getName() + "#alpha\"},"
                + "{\"id\":777}]}");
        assertEquals(Arrays.asList("alpha", "pinnedByAnnotation"), run(new MockartyTestPlanFilter(p)));
    }

    @Test
    @DisplayName("an unlisted test is excluded with a reason naming the plan")
    void exclusionCarriesAReason() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[{\"id\":\"nope\"}]}");
        assertTrue(run(new MockartyTestPlanFilter(p)).isEmpty(),
                "no fixture matches the plan, so none may run");
    }

    @Test
    @DisplayName("the headline bug: an EMPTY plan refuses the run instead of passing green")
    void emptyPlanRefusesTheRun() {
        AllureTestPlan p = plan("{\"version\":\"1.0\",\"tests\":[]}");
        MockartyTestPlanFilter filter = new MockartyTestPlanFilter(p);
        MockartyTestPlanException e = assertThrows(MockartyTestPlanException.class,
                () -> filter.apply(new StubDescriptor()));
        assertTrue(e.getMessage().contains("EMPTY"), e.getMessage());
        assertTrue(e.getMessage().contains("NOT a pass"), e.getMessage());
    }

    @Test
    @DisplayName("a plan carrying no path is inert")
    void nullPlanIsInert() {
        MockartyTestPlanFilter filter = new MockartyTestPlanFilter((AllureTestPlan) null);
        assertNull(filter.getPlan());
        assertTrue(filter.apply(new StubDescriptor()).included());
    }

    /** Minimal TestDescriptor for the plan-level (pre-matching) assertions. */
    private static final class StubDescriptor
            extends org.junit.platform.engine.support.descriptor.AbstractTestDescriptor {

        StubDescriptor() {
            super(org.junit.platform.engine.UniqueId.forEngine("stub"), "stub");
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }
    }
}
