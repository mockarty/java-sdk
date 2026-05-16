// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

/**
 * JUnit 5 extension that auto-starts every static {@link MockartyContainer}
 * field declared on a test class and stops it after the test class
 * finishes.
 *
 * <p>Usage:</p>
 * <pre>
 * &#64;ExtendWith(MockartyContainerExtension.class)
 * class MyIntegrationTest {
 *     static final MockartyContainer mockarty = new MockartyContainer()
 *         .withFormat(Format.MOCKARTY);
 *
 *     &#64;Test
 *     void exampleTest() {
 *         mockarty.apply(...);
 *         ...
 *     }
 * }
 * </pre>
 *
 * <p>The extension also self-registers via {@code META-INF/services/}
 * for projects that enable
 * {@code junit.jupiter.extensions.autodetection.enabled=true} — in that
 * case the {@code @ExtendWith} is implicit.</p>
 *
 * <p>If Docker is not reachable the extension is a no-op: every
 * container field is left un-started so the user's test body sees the
 * failure on first method call (instead of a hard fail in
 * {@code BeforeAll} that masks the real root cause).</p>
 */
public class MockartyContainerExtension implements BeforeAllCallback, AfterAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        forEachContainerField(context, (field, container) -> {
            if (!container.isRunning()) {
                container.start();
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        forEachContainerField(context, (field, container) -> {
            if (container.isRunning()) {
                container.stop();
            }
        });
    }

    private void forEachContainerField(ExtensionContext ctx, FieldAction action) {
        Optional<Class<?>> klass = ctx.getTestClass();
        if (klass.isEmpty()) {
            return;
        }
        for (Field f : klass.get().getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (!MockartyContainer.class.isAssignableFrom(f.getType())) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object value = f.get(null);
                if (value instanceof MockartyContainer mc) {
                    action.run(f, mc);
                }
            } catch (IllegalAccessException e) {
                throw new MockartyContainerException(
                    "MockartyContainerExtension: cannot access static field " + f.getName(), e);
            }
        }
    }

    @FunctionalInterface
    private interface FieldAction {
        void run(Field f, MockartyContainer c);
    }
}
