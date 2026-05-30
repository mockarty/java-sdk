// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testng;

import org.testng.IClassListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import ru.mockarty.junit5.allure.AllureLifecycle;
import ru.mockarty.junit5.allure.AllureModel;
import ru.mockarty.junit5.allure.Labels;
import ru.mockarty.junit5.framework.AllureMirror;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * TestNG-side bridge into the Mockarty Allure pipeline.
 *
 * <p>Mirror image of {@code MockartyExtension} for JUnit5: every TestNG
 * test method drives the same {@link AllureLifecycle}, so a project that
 * runs JUnit5 + TestNG side-by-side emits one coherent
 * {@code allure-results/} directory.</p>
 *
 * <h2>Lifecycle hooks</h2>
 * <ul>
 *   <li>{@link #onBeforeClass} → start a Container for the test class.</li>
 *   <li>{@link #beforeInvocation} (only for {@code @Test} methods) →
 *       open the {@link AllureLifecycle.Context} test result, harvest
 *       Allure annotations off the method/class, push parameters from
 *       the TestNG data-provider iteration.</li>
 *   <li>{@link #afterInvocation} → mark status (passed / failed /
 *       skipped) and finalise the TestResult file.</li>
 *   <li>{@link #onAfterClass} → stop the Container, write
 *       {@code <uuid>-container.json}.</li>
 * </ul>
 *
 * <p>Registration: add this class to {@code testng.xml} via
 * {@code <listeners><listener class-name="ru.mockarty.testng.MockartyTestNgListener"/></listeners>},
 * or — preferred — let TestNG auto-discover it via the
 * {@code META-INF/services/org.testng.ITestNGListener} SPI file we
 * ship in this module.</p>
 *
 * <h2>Thread safety</h2>
 * <p>TestNG runs methods on worker threads. The listener stores the
 * per-test-class container UUID in a {@link ConcurrentMap} keyed by
 * the class FQN, and routes everything else through the per-thread
 * {@link AllureLifecycle.Context}.</p>
 */
public final class MockartyTestNgListener
        implements ITestListener, IInvokedMethodListener, IClassListener {

    /** className → container UUID. */
    private final ConcurrentMap<String, String> classContainers = new ConcurrentHashMap<>();

    @Override
    public void onBeforeClass(ITestClass testClass) {
        String name = testClass.getName();
        String uuid = AllureLifecycle.get().startContainer(name);
        classContainers.put(name, uuid);
    }

    @Override
    public void onAfterClass(ITestClass testClass) {
        String name = testClass.getName();
        String uuid = classContainers.remove(name);
        if (uuid != null) {
            AllureLifecycle.get().stopContainer(uuid);
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        Method jm = method.getTestMethod().getConstructorOrMethod().getMethod();
        if (jm == null) {
            return;
        }
        AllureLifecycle lc = AllureLifecycle.get();
        String displayName = jm.getName();
        String fullName = jm.getDeclaringClass().getName() + "." + jm.getName();
        AllureModel.TestResult tr = lc.startTest(displayName, fullName);

        // Parameters: TestNG data-providers populate result.getParameters()
        // with an Object[]; index → name="p{i}" matches allure-testng.
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            StringBuilder paramSig = new StringBuilder();
            for (int i = 0; i < params.length; i++) {
                String name = "p" + i;
                String value = params[i] == null ? "null" : params[i].toString();
                lc.addParameter(name, value);
                paramSig.append(name).append('=').append(value).append(';');
            }
            tr.historyId = AllureLifecycle.stableHistoryId(fullName, paramSig.toString());
        }

        // Default labels.
        lc.addLabel(Labels.LANGUAGE, "java");
        lc.addLabel(Labels.FRAMEWORK, "testng");
        lc.addLabel(Labels.THREAD, Thread.currentThread().getName());
        lc.addLabel(Labels.PACKAGE, jm.getDeclaringClass().getPackage() == null
                ? "" : jm.getDeclaringClass().getPackage().getName());
        lc.addLabel(Labels.TEST_CLASS, jm.getDeclaringClass().getName());
        lc.addLabel(Labels.TEST_METHOD, jm.getName());
        lc.addLabel(Labels.SUITE, jm.getDeclaringClass().getName());

        // TestNG @Test(groups=) → Allure tag.
        org.testng.annotations.Test testAnn = jm.getAnnotation(org.testng.annotations.Test.class);
        if (testAnn != null) {
            for (String g : testAnn.groups()) {
                lc.addLabel(Labels.TAG, g);
            }
            if (!testAnn.description().isEmpty()) {
                lc.setDescription(testAnn.description());
            }
        }

        // Allure annotation harvest (mirror-mode).
        AllureMirror.applyToLifecycle(AllureMirror.harvest(jm));

        // Register on the container.
        String containerUuid = classContainers.get(jm.getDeclaringClass().getName());
        if (containerUuid != null) {
            lc.addContainerChild(containerUuid, tr.uuid);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        AllureLifecycle lc = AllureLifecycle.get();
        if (!lc.context().hasTest()) {
            return;
        }
        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                lc.markPassed();
                break;
            case ITestResult.FAILURE:
                lc.markFailed(result.getThrowable());
                break;
            case ITestResult.SKIP:
                lc.markSkipped(result.getThrowable() == null
                        ? null : result.getThrowable().getMessage());
                break;
            default:
                lc.markFailed(result.getThrowable());
                break;
        }
        lc.stopTest();
    }

    // ── ITestListener (we only use it for skip-by-config + edge cases) ──

    @Override public void onTestStart(ITestResult result) { /* handled in beforeInvocation */ }
    @Override public void onTestSuccess(ITestResult result) { /* handled in afterInvocation */ }
    @Override public void onTestFailure(ITestResult result) { /* handled in afterInvocation */ }
    @Override public void onTestSkipped(ITestResult result) { /* handled in afterInvocation */ }
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}
    @Override public void onStart(ITestContext context) {}
    @Override public void onFinish(ITestContext context) {}
}
