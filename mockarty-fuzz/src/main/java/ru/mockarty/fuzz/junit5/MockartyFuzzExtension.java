// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.fuzz.junit5;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import ru.mockarty.fuzz.Runner;
import ru.mockarty.fuzz.Target;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * JUnit5 extension that wires the fuzz DSL into the test lifecycle.
 *
 * <p>For each {@code @Test} method in a {@link MockartyFuzz}-annotated
 * class:</p>
 * <ol>
 *   <li>Invokes the {@link FuzzBuilder} method to produce a
 *       {@link Target}.</li>
 *   <li>Builds a {@link Runner} from the annotation's adminUrl / namespace
 *       / apiToken triple.</li>
 *   <li>Injects {@link Target} and {@link Runner} as test parameters on
 *       demand.</li>
 * </ol>
 *
 * <p>Auto-registered via JUnit's {@code Extension} SPI (see
 * {@code META-INF/services/...}); users only need {@link MockartyFuzz} on
 * their test class, no {@code @ExtendWith} required.</p>
 */
public final class MockartyFuzzExtension implements BeforeEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(MockartyFuzzExtension.class);
    private static final String TARGET_KEY = "mockarty-fuzz-target";
    private static final String RUNNER_KEY = "mockarty-fuzz-runner";
    private static final String LOCAL_KEY = "mockarty-fuzz-local-spawn";

    @Override
    public void beforeEach(ExtensionContext ctx) {
        Class<?> testClass = ctx.getRequiredTestClass();
        MockartyFuzz annotation = testClass.getAnnotation(MockartyFuzz.class);
        if (annotation == null) {
            // Engaged via @ExtendWith bare — let the user build their
            // own Target inside the test then.
            return;
        }
        Method builder = findBuilder(testClass);
        if (builder == null) {
            throw new IllegalStateException(
                    "@MockartyFuzz on " + testClass.getName()
                            + " requires a static @FuzzBuilder method returning Target");
        }
        Object built;
        try {
            builder.setAccessible(true);
            built = builder.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke @FuzzBuilder method " + builder, e);
        }
        if (!(built instanceof Target target)) {
            throw new IllegalStateException(
                    "@FuzzBuilder method must return Target, got "
                            + (built == null ? "null" : built.getClass().getName()));
        }
        Runner runner = Runner.builder()
                .adminUrl(annotation.adminUrl())
                .namespace(annotation.namespace())
                .apiToken(annotation.apiToken())
                .build();
        ExtensionContext.Store store = ctx.getStore(NS);
        store.put(TARGET_KEY, target);
        store.put(RUNNER_KEY, runner);
        store.put(LOCAL_KEY, annotation.localSpawn());
    }

    @Override
    public boolean supportsParameter(ParameterContext p, ExtensionContext e) {
        Class<?> type = p.getParameter().getType();
        return type == Target.class || type == Runner.class || type == Boolean.class;
    }

    @Override
    public Object resolveParameter(ParameterContext p, ExtensionContext e)
            throws ParameterResolutionException {
        ExtensionContext.Store store = e.getStore(NS);
        Class<?> type = p.getParameter().getType();
        Object got = switch (type.getSimpleName()) {
            case "Target" -> store.get(TARGET_KEY, Target.class);
            case "Runner" -> store.get(RUNNER_KEY, Runner.class);
            case "Boolean" -> store.get(LOCAL_KEY, Boolean.class);
            default -> null;
        };
        if (got == null) {
            throw new ParameterResolutionException(
                    "MockartyFuzzExtension: no " + type.getSimpleName()
                            + " in context — did you annotate the test class with "
                            + "@MockartyFuzz and provide a static @FuzzBuilder method?");
        }
        return got;
    }

    private static Method findBuilder(Class<?> testClass) {
        Method match = null;
        for (Method m : testClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(FuzzBuilder.class)) continue;
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalStateException("@FuzzBuilder method " + m + " must be static");
            }
            if (m.getParameterCount() != 0) {
                throw new IllegalStateException("@FuzzBuilder method " + m + " must take no parameters");
            }
            if (m.getReturnType() != Target.class) {
                throw new IllegalStateException(
                        "@FuzzBuilder method " + m + " must return Target, got " + m.getReturnType());
            }
            if (match != null) {
                throw new IllegalStateException(
                        "Multiple @FuzzBuilder methods on " + testClass.getName()
                                + " — exactly one is allowed");
            }
            match = m;
        }
        return match;
    }
}
