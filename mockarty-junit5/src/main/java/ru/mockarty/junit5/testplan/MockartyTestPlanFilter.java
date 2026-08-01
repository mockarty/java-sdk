// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Restricts a JUnit Platform run to the tests listed in the Allure test plan
 * ({@code ALLURE_TESTPLAN_PATH}) — the selective-execution contract Allure
 * TestOps and {@code mockarty-cli allure rerun-failed} drive.
 *
 * <p>Auto-registered through the JUnit Platform {@code ServiceLoader}
 * mechanism, so a suite gets selective execution from the dependency alone —
 * no launcher configuration. When no plan is configured it is inert.</p>
 *
 * <h2>What is matched</h2>
 *
 * <p>A plan entry's {@code id} is compared against every Allure id the test
 * carries:</p>
 * <ul>
 *   <li>an {@code @AllureId(...)} annotation on the test method (read
 *       reflectively, so Allure need not be on the classpath);</li>
 *   <li>a JUnit {@code @Tag} of the form {@code allure.id:123} /
 *       {@code @allure.id=123} — the tag shape TestOps writes;</li>
 *   <li>{@code @TestCase("CASE-1")} from this SDK, so a Mockarty-native
 *       suite is addressable by its TCM case id too.</li>
 * </ul>
 *
 * <p>A plan entry's {@code selector} is compared against every unique name
 * the test can be addressed by: the JUnit unique id, {@code Class#method},
 * {@code Class#method(paramTypes)}, the dotted {@code package.Class.method}
 * form the Allure TestOps docs use, and the bare class name for
 * class-level descriptors.</p>
 *
 * <h2>Failure modes</h2>
 *
 * <p>A missing, unreadable or malformed plan, and a plan that selects
 * nothing, both raise {@link MockartyTestPlanException} — discovery fails
 * loudly instead of degrading into a silent full run (or a green build that
 * executed zero tests). Set {@code MOCKARTY_TESTPLAN_MODE=off} to opt out
 * of plan consumption entirely.</p>
 */
public class MockartyTestPlanFilter implements PostDiscoveryFilter {

    /** {@code @Tag("allure.id:123")} / {@code @Tag("@allure.id=123")}. */
    private static final Pattern ID_TAG = Pattern.compile("^@?allure\\.id[:=](?<id>.+)$");

    /** Annotation simple-names understood as "this is the Allure id". */
    private static final Set<String> ID_ANNOTATIONS = new LinkedHashSet<>(
            java.util.Arrays.asList("AllureId", "AllureID"));

    private final AllureTestPlan plan;
    private final RuntimeException loadFailure;
    private volatile boolean emptyPlanReported;

    /** Auto-registration constructor: reads the plan from the environment. */
    public MockartyTestPlanFilter() {
        AllureTestPlan loaded = null;
        RuntimeException failure = null;
        try {
            loaded = AllureTestPlanLoader.fromEnvironment();
        } catch (RuntimeException e) {
            // Deferred: throwing from a ServiceLoader-instantiated constructor
            // surfaces as an opaque ServiceConfigurationError. Rethrown from
            // apply(), the user sees our message.
            failure = e;
        }
        this.plan = loaded;
        this.loadFailure = failure;
    }

    /**
     * Testing / embedding constructor.
     *
     * @param plan the plan to enforce, or null to disable filtering.
     */
    public MockartyTestPlanFilter(AllureTestPlan plan) {
        this.plan = plan;
        this.loadFailure = null;
    }

    /** {@inheritDoc} */
    @Override
    public FilterResult apply(TestDescriptor descriptor) {
        if (loadFailure != null) {
            throw loadFailure;
        }
        if (plan == null) {
            return FilterResult.included("no Allure test plan configured");
        }
        if (plan.isEmpty()) {
            reportEmptyPlanOnce();
            throw new MockartyTestPlanException(
                    "The Allure test plan " + plan.getPath() + " is EMPTY (\"tests\": []) — it selects no "
                            + "tests, so nothing would be executed. This run would prove nothing; it is NOT "
                            + "a pass. Fix the plan, or set " + AllureTestPlanLoader.ENV_TESTPLAN_MODE
                            + "=off to run the whole suite deliberately.");
        }
        if (descriptor == null) {
            return FilterResult.included("not a test");
        }
        // Containers are descended into; only leaves are selected against.
        if (!descriptor.getChildren().isEmpty() || !descriptor.isTest()) {
            return FilterResult.included("filter only applies to tests");
        }
        if (plan.matches(allureIds(descriptor), selectors(descriptor))) {
            return FilterResult.included("selected by the Allure test plan");
        }
        return FilterResult.excluded("not listed in the Allure test plan " + plan.getPath());
    }

    private void reportEmptyPlanOnce() {
        if (emptyPlanReported) {
            return;
        }
        emptyPlanReported = true;
        System.err.println("mockarty: the Allure test plan " + plan.getPath()
                + " is EMPTY (\"tests\": []) — no test can be selected, so the run is refused.");
    }

    /**
     * Every Allure id the descriptor can be addressed by.
     *
     * @param descriptor the discovered test.
     * @return ids, possibly empty, never null.
     */
    static Collection<String> allureIds(TestDescriptor descriptor) {
        Set<String> ids = new LinkedHashSet<>();
        Method method = testMethod(descriptor);
        if (method != null) {
            for (Annotation annotation : method.getAnnotations()) {
                String simple = annotation.annotationType().getSimpleName();
                if (ID_ANNOTATIONS.contains(simple)) {
                    addNonBlank(ids, annotationValue(annotation));
                } else if ("TestCase".equals(simple)) {
                    // ru.mockarty.junit5.framework.TestCase("CASE-1")
                    addNonBlank(ids, annotationValue(annotation));
                }
            }
        }
        for (TestTag tag : descriptor.getTags()) {
            Matcher matcher = ID_TAG.matcher(tag.getName());
            if (matcher.matches()) {
                addNonBlank(ids, matcher.group("id"));
            }
        }
        return ids;
    }

    /**
     * Every selector string the descriptor can be addressed by.
     *
     * @param descriptor the discovered test.
     * @return selectors, possibly empty, never null.
     */
    static Collection<String> selectors(TestDescriptor descriptor) {
        Set<String> out = new LinkedHashSet<>();
        addNonBlank(out, descriptor.getUniqueId() == null ? null : descriptor.getUniqueId().toString());

        TestSource source = descriptor.getSource().orElse(null);
        if (source instanceof MethodSource) {
            MethodSource ms = (MethodSource) source;
            String className = ms.getClassName();
            String methodName = ms.getMethodName();
            String params = ms.getMethodParameterTypes();
            if (className != null && methodName != null) {
                // Class#method(paramTypes) is the shape DiscoveryManifestAssembler
                // reports as fullName for a method WITH parameters, so a
                // Mockarty-generated plan matches it verbatim. The empty-parens
                // form is emitted too, since some generators always append them.
                addNonBlank(out, className + "#" + methodName + "(" + (params == null ? "" : params) + ")");
                addNonBlank(out, className + "#" + methodName);
                addNonBlank(out, className + "." + methodName);
                addNonBlank(out, simpleName(className) + "#" + methodName);
                addNonBlank(out, simpleName(className) + "." + methodName);
            }
            addNonBlank(out, className);
        } else if (source instanceof ClassSource) {
            String className = ((ClassSource) source).getClassName();
            addNonBlank(out, className);
            addNonBlank(out, simpleName(className));
        }
        return out;
    }

    private static Method testMethod(TestDescriptor descriptor) {
        TestSource source = descriptor.getSource().orElse(null);
        if (!(source instanceof MethodSource)) {
            return null;
        }
        try {
            return ((MethodSource) source).getJavaMethod();
        } catch (RuntimeException e) {
            // The class may not be loadable in this context (engine-specific
            // sources). Tag- and name-based matching still applies.
            return null;
        }
    }

    /** Read {@code value()} off an annotation without compiling against it. */
    private static String annotationValue(Annotation annotation) {
        try {
            Object value = annotation.annotationType().getMethod("value").invoke(annotation);
            return value == null ? null : String.valueOf(value).trim();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static String simpleName(String className) {
        if (className == null) {
            return null;
        }
        int dot = className.lastIndexOf('.');
        return dot >= 0 ? className.substring(dot + 1) : className;
    }

    private static void addNonBlank(Collection<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.add(value.trim());
        }
    }

    /** @return the plan being enforced, or null when filtering is inert. */
    public AllureTestPlan getPlan() {
        return plan;
    }
}
