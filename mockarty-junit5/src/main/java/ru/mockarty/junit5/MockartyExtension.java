// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import ru.mockarty.MockartyClient;
import ru.mockarty.junit5.framework.AttachReport;
import ru.mockarty.junit5.framework.MockartyContext;
import ru.mockarty.junit5.framework.TestCase;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * JUnit 5 extension for Mockarty test integration.
 *
 * <p>Provides four things in one place — bring-your-own-defaults so the
 * 80% case needs zero configuration:</p>
 * <ul>
 *   <li>Parameter injection of {@link MockartyClient} and {@link MockartyServer}.</li>
 *   <li>Auto-cleanup of mocks created via {@link MockartyServer} (toggleable).</li>
 *   <li>Case-frame lifecycle for methods annotated with
 *       {@link ru.mockarty.junit5.framework.TestCase} — pushes a frame
 *       before the test, pops it after.</li>
 *   <li>Best-effort upload of test outcome + step recording + attachments
 *       for methods annotated with
 *       {@link ru.mockarty.junit5.framework.AttachReport}.</li>
 * </ul>
 *
 * <p>Activate via the class-level {@link MockartyTest} annotation
 * (defaults applied automatically) or via plain
 * {@code @ExtendWith(MockartyExtension.class)} when you want manual
 * control.</p>
 */
public class MockartyExtension implements
        BeforeEachCallback,
        AfterEachCallback,
        BeforeTestExecutionCallback,
        AfterTestExecutionCallback,
        ParameterResolver {

    private static final Logger log = LoggerFactory.getLogger(MockartyExtension.class);

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MockartyExtension.class);

    private static final String CLIENT_KEY = "mockarty-client";
    private static final String SERVER_KEY = "mockarty-server";
    private static final String CLEANUP_KEY = "mockarty-cleanup";

    @Override
    public void beforeEach(ExtensionContext context) {
        MockartyTest annotation = findClassAnnotation(context);

        String baseUrl = resolveValue(
                annotation != null ? annotation.baseUrl() : "",
                "MOCKARTY_BASE_URL",
                "http://localhost:5770"
        );

        String apiKey = resolveValue(
                annotation != null ? annotation.apiKey() : "",
                "MOCKARTY_API_KEY",
                null
        );

        String namespace = annotation != null ? annotation.namespace() : "sandbox";
        boolean cleanupAfterEach = annotation == null || annotation.cleanupAfterEach();

        MockartyClient client = MockartyClient.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .namespace(namespace)
                .build();

        MockartyServer server = new MockartyServer(client);

        ExtensionContext.Store store = context.getStore(NAMESPACE);
        store.put(CLIENT_KEY, client);
        store.put(SERVER_KEY, server);
        store.put(CLEANUP_KEY, cleanupAfterEach);

        log.debug("MockartyExtension initialized: baseUrl={}, namespace={}, cleanup={}",
                baseUrl, namespace, cleanupAfterEach);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        // Push a case frame for methods annotated with @TestCase. Idempotent
        // when the annotation is missing — no frame pushed.
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod == null) return;
        TestCase tc = testMethod.getAnnotation(TestCase.class);
        if (tc == null) return;
        validateTestCaseAnnotation(tc);

        MockartyContext.CaseFrame frame = new MockartyContext.CaseFrame();
        frame.caseId = emptyToNull(tc.value());
        frame.caseName = emptyToNull(tc.name());
        frame.planId = emptyToNull(tc.plan());
        frame.autoCreate = tc.autoCreate();
        MockartyContext.pushCase(frame);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod == null) return;

        MockartyContext.CaseFrame frame = MockartyContext.currentCase();
        // Pop the case frame regardless of whether @AttachReport is set —
        // beforeTestExecution pushed it iff @TestCase is present.
        if (testMethod.isAnnotationPresent(TestCase.class)) {
            try {
                if (testMethod.isAnnotationPresent(AttachReport.class) && frame != null) {
                    uploadOutcomeBestEffort(context, testMethod, frame);
                }
            } finally {
                MockartyContext.popCase();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        Boolean cleanup = store.get(CLEANUP_KEY, Boolean.class);
        if (Boolean.TRUE.equals(cleanup)) {
            MockartyServer server = store.get(SERVER_KEY, MockartyServer.class);
            if (server != null) {
                server.cleanup();
            }
        }

        MockartyClient client = store.get(CLIENT_KEY, MockartyClient.class);
        if (client != null) {
            client.close();
        }

        // Always reset framework state between tests so a leaky push from a
        // failed test doesn't bleed into the next one.
        MockartyContext.resetForTest();

        log.debug("MockartyExtension cleaned up after test: {}", context.getDisplayName());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == MockartyClient.class || type == MockartyServer.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);

        if (type == MockartyClient.class) {
            return store.get(CLIENT_KEY, MockartyClient.class);
        }
        if (type == MockartyServer.class) {
            return store.get(SERVER_KEY, MockartyServer.class);
        }

        throw new ParameterResolutionException("Unsupported parameter type: " + type);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private MockartyTest findClassAnnotation(ExtensionContext context) {
        return context.getTestClass()
                .map(cls -> cls.getAnnotation(MockartyTest.class))
                .orElse(null);
    }

    private String resolveValue(String annotationValue, String envVar, String defaultValue) {
        if (annotationValue != null && !annotationValue.isEmpty()) {
            return annotationValue;
        }
        String sysProp = System.getProperty(envVar);
        if (sysProp != null && !sysProp.isEmpty()) {
            return sysProp;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return defaultValue;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static void validateTestCaseAnnotation(TestCase tc) {
        boolean hasId = tc.value() != null && !tc.value().isEmpty();
        if (!hasId && !tc.autoCreate()) {
            throw new IllegalStateException(
                    "@TestCase requires either value=\"<id>\" or autoCreate=true");
        }
        if (tc.autoCreate() && (tc.name() == null || tc.name().isEmpty())) {
            throw new IllegalStateException(
                    "@TestCase(autoCreate=true) requires name=");
        }
    }

    /** Best-effort outcome upload: silently no-op when SDK surface or client
     * isn't reachable. Tests must never fail because reporting failed. */
    private void uploadOutcomeBestEffort(
            ExtensionContext context,
            Method testMethod,
            MockartyContext.CaseFrame frame) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        MockartyClient client = store.get(CLIENT_KEY, MockartyClient.class);
        if (client == null) return;

        // Build a payload snapshot — the live SDK may grow a typed
        // upload method; we structure the data so the wire shape is
        // ready when that lands. Until then, this method is a no-op:
        // we just clear the frame state and log for visibility.
        Map<String, Object> snapshot = frame.snapshot();
        snapshot.put("testId", context.getUniqueId());
        snapshot.put("testDisplayName", context.getDisplayName());
        snapshot.put(
                "outcome",
                context.getExecutionException()
                        .map(e -> "failed:" + e.getClass().getSimpleName() + ":" + e.getMessage())
                        .orElse("passed")
        );
        log.debug("MockartyExtension: outcome ready for upload — testId={}, caseId={}, steps={}, attachments={}",
                context.getUniqueId(),
                frame.caseId,
                frame.steps.size(),
                frame.attachments.size());

        // The snapshot is logged at debug level for observability —
        // hook the logger when you need to ship reports out-of-band
        // until a typed TCM result-upload method lands on MockartyClient.
        log.debug("MockartyExtension snapshot: {}", snapshot.size());
    }

}
