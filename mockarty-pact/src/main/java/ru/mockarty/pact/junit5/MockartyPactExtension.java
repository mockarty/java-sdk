// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.pact.junit5;

import ru.mockarty.pact.Consumer;
import ru.mockarty.pact.MockServer;
import ru.mockarty.pact.Pact;
import ru.mockarty.pact.SpecVersion;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JUnit5 extension that wires the Pact DSL into the test lifecycle.
 *
 * <p>Activates on test classes annotated with {@link PactConsumer}. For
 * each {@code @Test} method:</p>
 * <ol>
 *   <li>Invokes the {@code static}, {@link PactBuilder}-annotated method
 *       on the test class to obtain a {@link Consumer} or {@link Pact}.</li>
 *   <li>Applies the consumer/provider/specVersion/outputDir defaults from
 *       {@link PactConsumer} when the builder hasn't already set them.</li>
 *   <li>Starts a {@link MockServer} bound to an ephemeral port.</li>
 *   <li>Injects the {@link MockServer} (or its base URI) as a test
 *       parameter.</li>
 *   <li>After the test, calls {@link MockServer#verify()} (only when the
 *       test passed — failed tests aren't asked to verify their pacts).</li>
 *   <li>Closes the server, which writes the pact.json under
 *       {@link Pact#outputDir()}.</li>
 * </ol>
 *
 * <p>Auto-registered via JUnit's {@code AutoCloseable} SPI under
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension} —
 * the user only needs the {@link PactConsumer} annotation, no
 * {@code @ExtendWith} declaration.</p>
 */
public final class MockartyPactExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NS =
            ExtensionContext.Namespace.create(MockartyPactExtension.class);
    private static final String SERVER_KEY = "mockarty-pact-server";
    private static final String PACT_KEY = "mockarty-pact-contract";

    @Override
    public void beforeEach(ExtensionContext ctx) {
        Class<?> testClass = ctx.getRequiredTestClass();
        PactConsumer annotation = testClass.getAnnotation(PactConsumer.class);
        if (annotation == null) {
            // Engaged via @ExtendWith without the convenience annotation.
            // The user must build the pact manually inside the test then.
            return;
        }

        Method builderMethod = findBuilderMethod(testClass);
        if (builderMethod == null) {
            throw new IllegalStateException(
                    "@PactConsumer on " + testClass.getName()
                            + " requires a static @PactBuilder method returning Consumer or Pact");
        }

        Object built;
        try {
            builderMethod.setAccessible(true);
            built = builderMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to invoke @PactBuilder method " + builderMethod, e);
        }

        Pact pact;
        if (built instanceof Pact p) {
            pact = p;
        } else if (built instanceof Consumer c) {
            applyDefaults(c, annotation);
            pact = c.build();
        } else {
            throw new IllegalStateException(
                    "@PactBuilder method must return Consumer or Pact, got "
                            + (built == null ? "null" : built.getClass().getName()));
        }

        MockServer server = MockServer.start(pact);
        ExtensionContext.Store store = ctx.getStore(NS);
        store.put(PACT_KEY, pact);
        store.put(SERVER_KEY, server);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        ExtensionContext.Store store = ctx.getStore(NS);
        MockServer server = store.get(SERVER_KEY, MockServer.class);
        if (server == null) return;
        try {
            // Only verify on green tests — a failed assertion in the user
            // code is the primary failure signal; we don't want to drown
            // it under a pact-verification trace from the same test.
            if (ctx.getExecutionException().isEmpty()) {
                server.verify();
            }
        } finally {
            server.close();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext p, ExtensionContext e) {
        Class<?> type = p.getParameter().getType();
        return type == MockServer.class
                || type == Pact.class
                || type == java.net.URI.class;
    }

    @Override
    public Object resolveParameter(ParameterContext p, ExtensionContext e)
            throws ParameterResolutionException {
        Class<?> type = p.getParameter().getType();
        ExtensionContext.Store store = e.getStore(NS);
        MockServer server = store.get(SERVER_KEY, MockServer.class);
        if (server == null) {
            throw new ParameterResolutionException(
                    "MockartyPactExtension: no MockServer in context — "
                            + "did you annotate the test class with @PactConsumer "
                            + "and provide a static @PactBuilder method?");
        }
        if (type == MockServer.class) return server;
        if (type == Pact.class) return store.get(PACT_KEY, Pact.class);
        if (type == java.net.URI.class) return server.uri();
        throw new ParameterResolutionException("Unsupported parameter type: " + type);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static Method findBuilderMethod(Class<?> testClass) {
        for (Method m : testClass.getDeclaredMethods()) {
            if (!m.isAnnotationPresent(PactBuilder.class)) continue;
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new IllegalStateException(
                        "@PactBuilder method " + m + " must be static");
            }
            if (m.getParameterCount() != 0) {
                throw new IllegalStateException(
                        "@PactBuilder method " + m + " must take no parameters");
            }
            Class<?> rt = m.getReturnType();
            if (rt != Consumer.class && rt != Pact.class) {
                throw new IllegalStateException(
                        "@PactBuilder method " + m + " must return Consumer or Pact, got " + rt);
            }
            return m;
        }
        return null;
    }

    private static void applyDefaults(Consumer c, PactConsumer ann) {
        // We don't override fields the builder lambda already set.
        if (c.providerName() == null) {
            c.withProvider(ann.provider());
        }
        // Spec version: the consumer's default is V4 too, but if the user
        // explicitly says SpecVersion.V3 on the annotation we still want
        // to apply it. We only override when the consumer is still on
        // its default V4 AND the annotation says non-V4.
        if (c.currentSpecVersion() == SpecVersion.V4 && ann.specVersion() != SpecVersion.V4) {
            c.specVersion(ann.specVersion());
        }
        if (c.currentOutputDir() == null) {
            String od = ann.outputDir();
            Path target = od.isEmpty() ? Paths.get("build", "pacts") : Paths.get(od);
            c.outputDir(target);
        }
    }
}
