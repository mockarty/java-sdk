// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import ru.mockarty.MockartyClient;
import ru.mockarty.junit5.framework.AttachReport;
import ru.mockarty.junit5.framework.MockartyContext;
import ru.mockarty.junit5.framework.TestCase;
import ru.mockarty.model.ExternalAttachment;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalStep;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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

    /** Best-effort outcome upload: POSTs to /tcm/external-runs. Any error
     * is logged at debug and swallowed — tests must never fail because
     * reporting failed. */
    private void uploadOutcomeBestEffort(
            ExtensionContext context,
            Method testMethod,
            MockartyContext.CaseFrame frame) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        MockartyClient client = store.get(CLIENT_KEY, MockartyClient.class);
        if (client == null) {
            return;
        }
        String namespace = "sandbox";
        try {
            namespace = client.getConfig().getNamespace();
            if (namespace == null || namespace.isEmpty()) {
                namespace = "sandbox";
            }
        } catch (Throwable ignored) {
            // older client builds — fall through to default
        }

        ExternalRunRequest req = buildExternalRunRequest(context, frame);
        try {
            client.externalRuns().report(namespace, req);
        } catch (Throwable t) {
            log.debug("MockartyExtension: outcome upload failed for {}: {}",
                    context.getUniqueId(), t.toString());
        }
    }

    private ExternalRunRequest buildExternalRunRequest(
            ExtensionContext context,
            MockartyContext.CaseFrame frame) {
        boolean failed = context.getExecutionException().isPresent();
        boolean skipped = false;
        if (failed) {
            Throwable t = context.getExecutionException().get();
            // org.opentest4j.TestAbortedException → skipped (Assumptions.assumeTrue)
            if (t.getClass().getName().endsWith("TestAbortedException")) {
                skipped = true;
                failed = false;
            }
        }
        String status = failed
                ? ExternalRunRequest.STATUS_FAILED
                : (skipped ? ExternalRunRequest.STATUS_SKIPPED : ExternalRunRequest.STATUS_PASSED);

        ExternalRunRequest req = new ExternalRunRequest()
                .status(status)
                .caseId(frame.caseId)
                .caseName(frame.caseName)
                .planId(frame.planId)
                .autoCreate(frame.autoCreate)
                .framework("junit5")
                .frameworkVersion(System.getProperty("java.specification.version", ""))
                .externalId(context.getUniqueId())
                .testDisplayName(context.getDisplayName());

        if (failed) {
            Throwable t = context.getExecutionException().get();
            req.error(t.getClass().getSimpleName() + ": " + safeMessage(t));
        }

        if (!frame.steps.isEmpty()) {
            List<ExternalStep> steps = new ArrayList<>(frame.steps.size());
            for (Map<String, Object> s : frame.steps) {
                ExternalStep es = new ExternalStep()
                        .name(asString(s.get("name")))
                        .status(asString(s.get("status")))
                        .error(asString(s.get("error")));
                Object dur = s.get("durationNanos");
                if (dur instanceof Long && (Long) dur > 0) {
                    es.durationMs(((Long) dur) / 1_000_000L);
                }
                Object md = s.get("metadata");
                if (md instanceof Map<?, ?>) {
                    Map<String, Object> typed = new HashMap<>();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) md).entrySet()) {
                        typed.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    es.metadata(typed);
                }
                steps.add(es);
            }
            req.steps(steps);
        }

        if (!frame.attachments.isEmpty()) {
            List<ExternalAttachment> wire = new ArrayList<>(frame.attachments.size());
            for (Map<String, Object> a : frame.attachments) {
                String name = asString(a.get("name"));
                String contentType = asString(a.get("contentType"));
                Object body = a.get("body");
                ExternalAttachment ea = new ExternalAttachment()
                        .name(name)
                        .contentType(contentType.isEmpty() ? "application/octet-stream" : contentType);
                if (body instanceof byte[]) {
                    ea.body((byte[]) body);
                } else if (body instanceof String) {
                    ea.body(((String) body).getBytes(StandardCharsets.UTF_8));
                } else if (body == null) {
                    ea.bodyB64(Base64.getEncoder().encodeToString(new byte[0]));
                } else {
                    ea.body(String.valueOf(body).getBytes(StandardCharsets.UTF_8));
                }
                wire.add(ea);
            }
            req.attachments(wire);
        }

        if (!frame.metadata.isEmpty()) {
            req.metadata(new HashMap<>(frame.metadata));
        }
        return req;
    }

    private static String asString(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String safeMessage(Throwable t) {
        String m = t.getMessage();
        return m == null ? "" : m;
    }

}
