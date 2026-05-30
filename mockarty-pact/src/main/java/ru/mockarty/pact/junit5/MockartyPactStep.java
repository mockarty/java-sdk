// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.junit5;

import java.lang.reflect.Method;

/**
 * Wrap a Pact consumer invocation as a named Allure step. Optional
 * shorthand for users who already have {@code mockarty-junit5} on the
 * classpath — the bridge is a thin reflective call into
 * {@code ru.mockarty.junit5.allure.AllureLifecycle}, so users without
 * the JUnit5 framework module pay no cost (the bridge silently no-ops).
 *
 * <pre>{@code
 * MockartyPactStep.named("verify users contract", () -> {
 *     mockServer.verify();
 * });
 * }</pre>
 *
 * <p>Why not just use {@code ru.mockarty.junit5.framework.Step}?
 * mockarty-junit5 is an optional dependency of this module
 * ({@code compileOnly} in {@code mockarty-pact/build.gradle.kts}), so we
 * can't reference the framework's {@code Step} class directly without
 * tightening the dependency edge. The reflective bridge keeps this
 * module self-contained.</p>
 */
public final class MockartyPactStep {

    private MockartyPactStep() {}

    public static void named(String name, Runnable body) {
        Object token = beginStep(name);
        boolean ok = true;
        try {
            body.run();
        } catch (RuntimeException | Error e) {
            ok = false;
            throw e;
        } finally {
            endStep(token, ok);
        }
    }

    public static <T> T named(String name, java.util.function.Supplier<T> body) {
        Object token = beginStep(name);
        boolean ok = true;
        try {
            return body.get();
        } catch (RuntimeException | Error e) {
            ok = false;
            throw e;
        } finally {
            endStep(token, ok);
        }
    }

    private static Object beginStep(String name) {
        try {
            Class<?> lc = Class.forName("ru.mockarty.junit5.allure.AllureLifecycle");
            Object instance = lc.getMethod("get").invoke(null);
            Method m = lc.getMethod("startStep", String.class);
            return m.invoke(instance, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static void endStep(Object token, boolean passed) {
        try {
            Class<?> lc = Class.forName("ru.mockarty.junit5.allure.AllureLifecycle");
            Object instance = lc.getMethod("get").invoke(null);
            Method m = lc.getMethod("stopStep", boolean.class);
            m.invoke(instance, passed);
        } catch (Throwable ignored) {
            // mockarty-junit5 not on the classpath — silent no-op.
        }
    }
}
