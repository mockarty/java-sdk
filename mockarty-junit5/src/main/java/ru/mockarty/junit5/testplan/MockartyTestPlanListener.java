// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * Reports a run that an Allure test plan narrowed down to nothing.
 *
 * <p>{@link MockartyTestPlanFilter} refuses an <em>empty</em> plan outright,
 * because that state is detectable while filtering. A plan that lists tests
 * but matches none of them is only detectable once discovery has finished —
 * and JUnit gives a {@code PostDiscoveryFilter} no such callback. This
 * listener fills that gap: when a plan was enforced and zero tests survived,
 * it says so loudly instead of letting the build report a green "0 tests".</p>
 *
 * <p>It cannot fail the build on its own — the JUnit Platform swallows
 * exceptions thrown by execution listeners by design. Turn the situation
 * into a failure at the build-tool level: Gradle's
 * {@code test { failOnNoDiscoveredTests = true }} or Maven Surefire's
 * {@code <failIfNoTests>true</failIfNoTests>}.</p>
 */
public class MockartyTestPlanListener implements TestExecutionListener {

    /** {@inheritDoc} */
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        String planPath = MockartyTestPlanFilter.activePlanPath();
        if (planPath == null || testPlan == null) {
            return;
        }
        if (testPlan.countTestIdentifiers(TestIdentifier::isTest) > 0) {
            return;
        }
        System.err.println();
        System.err.println("mockarty: the Allure test plan " + planPath
                + " matched NONE of the discovered tests — 0 tests will be executed.");
        System.err.println("mockarty: this run proves nothing; it is NOT a pass. Check that the plan's "
                + "'id'/'selector' values address this suite, or set "
                + AllureTestPlanLoader.ENV_TESTPLAN_MODE + "=off to run everything.");
        System.err.println("mockarty: to make this a build failure, use Gradle's "
                + "`test { failOnNoDiscoveredTests = true }` or Surefire's `<failIfNoTests>true</failIfNoTests>`.");
        System.err.println();
    }
}
