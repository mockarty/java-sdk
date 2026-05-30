// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testng;

import ru.mockarty.junit5.allure.AllureLifecycle;

/**
 * Thin user-facing step helper for TestNG suites. Mirror of
 * {@code ru.mockarty.junit5.framework.Step} but without the
 * MockartyContext case-frame coupling — TestNG users don't have a
 * {@code @TestCase}-equivalent annotation in this module, the bridge
 * goes straight to {@link AllureLifecycle}.
 *
 * <pre>{@code
 * try (MockartyStep s = MockartyStep.open("submit form")) {
 *     submit(...);
 * }
 * }</pre>
 */
public final class MockartyStep implements AutoCloseable {

    private boolean closed;
    private boolean failed;

    private MockartyStep(String name) {
        AllureLifecycle.get().startStep(name);
    }

    public static MockartyStep open(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("MockartyStep.open requires a non-empty name");
        }
        return new MockartyStep(name);
    }

    public static void run(String name, Runnable body) {
        try (MockartyStep s = open(name)) {
            try {
                body.run();
            } catch (RuntimeException | Error e) {
                s.failed = true;
                throw e;
            }
        }
    }

    public MockartyStep markFailed() {
        this.failed = true;
        return this;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        AllureLifecycle.get().stopStep(!failed);
    }
}
