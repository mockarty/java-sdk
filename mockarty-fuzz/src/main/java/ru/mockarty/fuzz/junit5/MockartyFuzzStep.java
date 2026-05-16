// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.fuzz.junit5;

import java.lang.reflect.Method;

/**
 * Reflective Allure-step bridge for fuzz iterations. Same shape as
 * {@code MockartyPactStep} — silent no-op when {@code mockarty-junit5}
 * isn't on the classpath.
 *
 * <p>Typical use: wrap each fuzz iteration in a named step so the
 * resulting Allure report shows one row per iteration with its own
 * status + attachments (request/response/finding).</p>
 *
 * <pre>{@code
 * for (Seed s : runner.seeds(target)) {
 *     MockartyFuzzStep.named("seed[" + s.id() + "]", () -> {
 *         runner.submitOne(target, s);
 *     });
 * }
 * }</pre>
 */
public final class MockartyFuzzStep {

    private MockartyFuzzStep() {}

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
