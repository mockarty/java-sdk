// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5;

import ru.mockarty.MockartyClient;
import ru.mockarty.junit5.framework.AllureMirror;
import ru.mockarty.junit5.framework.AttachReport;
import ru.mockarty.junit5.framework.MockartyContext;
import ru.mockarty.junit5.framework.MockartySuite;
import ru.mockarty.junit5.framework.TestCase;
import ru.mockarty.model.ExternalAttachment;
import ru.mockarty.model.ExternalRunRequest;
import ru.mockarty.model.ExternalStep;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import ru.mockarty.junit5.allure.AllureLifecycle;
import ru.mockarty.junit5.allure.AllureModel;
import ru.mockarty.junit5.allure.Labels;
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
        BeforeAllCallback,
        AfterAllCallback,
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
    /** True when {@link #beforeTestExecution} pushed a synthetic case frame
     * (because the test had no {@link TestCase} but did have Allure
     * annotations under mirror-mode). Tells {@link #afterTestExecution}
     * it must pop the frame even though the {@code @TestCase}-based path
     * is inactive. */
    private static final String SYNTHETIC_FRAME_KEY = "mockarty-allure-synthetic-frame";
    /** True when mirror-mode is enabled for the current test (resolved
     * from the class-level {@code @MockartyTest(mirrorAllure=...)}; default
     * is {@code true}). */
    private static final String MIRROR_ALLURE_KEY = "mockarty-mirror-allure";
    /** Per-test-class container UUID held in the parent ExtensionContext store. */
    private static final String CONTAINER_UUID_KEY = "mockarty-allure-container-uuid";

    @Override
    public void beforeAll(ExtensionContext context) {
        // Open an Allure Container that groups every test in the class —
        // matches allure-junit5 byte-for-byte. Stored on the class-level
        // ExtensionContext store so AfterAll closes the same container.
        String className = context.getTestClass()
                .map(Class::getName)
                .orElse(context.getDisplayName());
        String containerUuid = AllureLifecycle.get().startContainer(className);
        context.getStore(NAMESPACE).put(CONTAINER_UUID_KEY, containerUuid);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        String containerUuid = context.getStore(NAMESPACE)
                .get(CONTAINER_UUID_KEY, String.class);
        if (containerUuid != null) {
            AllureLifecycle.get().stopContainer(containerUuid);
        }
    }

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
        // Allure mirror-mode is default-ON (owner decision 2026-05-16,
        // SDK_FRAMEWORK_PLAN §3.3). When the user explicitly uses
        // {@code @MockartyTest(mirrorAllure = false)} we skip the
        // reflection harvest. When there's no {@code @MockartyTest} at
        // all (extension activated via {@code @ExtendWith}) we still
        // default to ON to match the SDK-wide invariant.
        boolean mirrorAllure = annotation == null || annotation.mirrorAllure();

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
        store.put(MIRROR_ALLURE_KEY, mirrorAllure);

        log.debug("MockartyExtension initialized: baseUrl={}, namespace={}, cleanup={}, mirrorAllure={}",
                baseUrl, namespace, cleanupAfterEach, mirrorAllure);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod == null) return;

        // Always start a fresh AllureLifecycle TestResult around every
        // JUnit test method — that's what produces <uuid>-result.json
        // for each invocation. Parameterized tests get distinct UUIDs
        // but a stable historyId so Allure aggregates retries correctly.
        Class<?> testCls = context.getTestClass().orElse(null);
        String displayName = context.getDisplayName();
        String fullName = testCls != null
                ? testCls.getName() + "." + testMethod.getName()
                : testMethod.getName();

        AllureLifecycle lc = AllureLifecycle.get();
        AllureModel.TestResult tr = lc.startTest(displayName, fullName);
        // Stable historyId: fullName + displayName-derived parameter hash so
        // @ParameterizedTest iterations collapse onto the same row across
        // retries but stay distinct from sibling iterations.
        String paramSig = displayName.equals(testMethod.getName())
                ? "" : displayName;
        tr.historyId = AllureLifecycle.stableHistoryId(fullName, paramSig);
        // Canonical Allure metadata labels matching allure-junit5.
        lc.addLabel(Labels.LANGUAGE, "java");
        lc.addLabel(Labels.FRAMEWORK, "junit5");
        lc.addLabel(Labels.THREAD, Thread.currentThread().getName());
        try {
            lc.addLabel(Labels.HOST, java.net.InetAddress.getLocalHost().getHostName());
        } catch (Throwable ignored) {
            // best-effort
        }
        if (testCls != null) {
            lc.addLabel(Labels.PACKAGE, testCls.getPackage() == null
                    ? "" : testCls.getPackage().getName());
            lc.addLabel(Labels.TEST_CLASS, testCls.getName());
            // @MockartySuite (method-level wins; otherwise class-level;
            // otherwise default to the test class FQN).
            MockartySuite ms = testMethod.getAnnotation(MockartySuite.class);
            if (ms == null) {
                ms = testCls.getAnnotation(MockartySuite.class);
            }
            if (ms != null && !ms.value().isEmpty()) {
                lc.addLabel(Labels.SUITE, ms.value());
            } else {
                lc.addLabel(Labels.SUITE, testCls.getName());
            }
            if (ms != null && !ms.parentSuite().isEmpty()) {
                lc.addLabel(Labels.PARENT_SUITE, ms.parentSuite());
            }
            if (ms != null && !ms.subSuite().isEmpty()) {
                lc.addLabel(Labels.SUB_SUITE, ms.subSuite());
            }
        }
        lc.addLabel(Labels.TEST_METHOD, testMethod.getName());
        // Register as child of the surrounding container.
        String containerUuid = context.getStore(NAMESPACE)
                .get(CONTAINER_UUID_KEY, String.class);
        if (containerUuid != null) {
            lc.addContainerChild(containerUuid, tr.uuid);
        }

        TestCase tc = testMethod.getAnnotation(TestCase.class);
        MockartyContext.CaseFrame frame = null;
        boolean synthetic = false;

        if (tc != null) {
            validateTestCaseAnnotation(tc);
            frame = new MockartyContext.CaseFrame();
            frame.caseId = emptyToNull(tc.value());
            frame.caseName = emptyToNull(tc.name());
            frame.planId = emptyToNull(tc.plan());
            frame.autoCreate = tc.autoCreate();
            // Phase 2.6 Mockarty extensions — see SDK_MOCKARTY_
            // EXTENSIONS_AUDIT.md. These ride the matching server-
            // side ExternalRunRequest fields landed in 1067beba.
            frame.description = emptyToNull(tc.description());
            frame.expectedResult = emptyToNull(tc.expectedResult());
            frame.claimOwnership = tc.claimOwnership();
            for (String raw : tc.customFields()) {
                if (raw == null || raw.isEmpty()) continue;
                // Each entry encoded as "type:name:value". Annotation
                // params can't be complex types so the colon-delimited
                // shape is the smallest serialisation that survives
                // through the @interface.
                java.util.Map<String, Object> cf = new java.util.HashMap<>();
                String[] parts = raw.split(":", 3);
                if (parts.length >= 1) cf.put("type", parts[0]);
                if (parts.length >= 2) cf.put("name", parts[1]);
                if (parts.length >= 3) cf.put("value", parts[2]);
                if (parts.length == 2) cf.put("value", "");
                frame.customFields.add(cf);
            }
            // Phase 2.6: emit annotation values as `mockarty:case:*`
            // labels so the CLI harvester (which mines Allure-result
            // labels for the Phase 2.6 fields) carries them through
            // to /tcm/external-runs. Without this Java tests can set
            // @TestCase(description=...) but the server never sees
            // those values — caught by SDK + CLI live smoke 2026-05-18.
            if (frame.description != null && !frame.description.isEmpty()) {
                lc.addLabel("mockarty:case:description", frame.description);
            }
            if (frame.expectedResult != null && !frame.expectedResult.isEmpty()) {
                lc.addLabel("mockarty:case:expected_result", frame.expectedResult);
            }
            if (frame.claimOwnership) {
                lc.addLabel("mockarty:case:claim_ownership", "true");
            }
            for (java.util.Map<String, Object> cf : frame.customFields) {
                Object type = cf.get("type");
                Object name = cf.get("name");
                Object value = cf.get("value");
                if (name == null || value == null) continue;
                // Match the CLI harvester pattern at allure.go:834 —
                // label name is exactly "mockarty:case:custom_field"
                // and the value encodes "type:name:value".
                String typeStr = type != null ? type.toString() : "string";
                lc.addLabel("mockarty:case:custom_field",
                        typeStr + ":" + name.toString() + ":" + value.toString());
            }
            MockartyContext.pushCase(frame);
        }

        // Allure mirror-mode: harvest @Step/@Severity/@Feature/@Story/... and
        // lift them onto the active case frame. For pure-Allure tests (no
        // @TestCase but Allure annotations present) we create a synthetic
        // frame so the metadata isn't dropped on the floor.
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        Boolean mirror = store.get(MIRROR_ALLURE_KEY, Boolean.class);
        if (Boolean.TRUE.equals(mirror)) {
            AllureMirror.Harvested h = AllureMirror.harvest(testMethod);
            if (!h.isEmpty()) {
                if (frame == null) {
                    frame = new MockartyContext.CaseFrame();
                    // Use the Allure @Title (or harvested classStepHint, or
                    // the JUnit display name) as the case name so the
                    // synthetic frame has something user-readable.
                    frame.caseName = h.title != null
                            ? h.title
                            : (h.classStepHint != null
                                    ? h.classStepHint
                                    : context.getDisplayName());
                    frame.autoCreate = false;
                    MockartyContext.pushCase(frame);
                    synthetic = true;
                }
                AllureMirror.apply(frame, h);
            }
            // Always feed harvested data into AllureLifecycle, regardless
            // of whether a CaseFrame exists — the on-disk Allure result
            // gets the labels/links/severity even when @TestCase is absent.
            AllureMirror.applyToLifecycle(AllureMirror.harvest(testMethod));
        }

        if (synthetic) {
            store.put(SYNTHETIC_FRAME_KEY, Boolean.TRUE);
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        Method testMethod = context.getTestMethod().orElse(null);
        if (testMethod == null) return;

        MockartyContext.CaseFrame frame = MockartyContext.currentCase();
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        boolean synthetic = Boolean.TRUE.equals(store.get(SYNTHETIC_FRAME_KEY, Boolean.class));

        // Reflect JUnit outcome into the AllureLifecycle.
        AllureLifecycle lc = AllureLifecycle.get();
        if (lc.context().hasTest()) {
            if (context.getExecutionException().isPresent()) {
                Throwable t = context.getExecutionException().get();
                if (t.getClass().getName().endsWith("TestAbortedException")) {
                    lc.markSkipped(t.getMessage());
                } else {
                    lc.markFailed(t);
                }
            } else {
                lc.markPassed();
            }
            // Always persist the result file — even tests without
            // @TestCase/@AttachReport get an allure-results entry.
            lc.stopTest();
        }

        // Pop the case frame regardless of whether @AttachReport is set —
        // beforeTestExecution pushed it iff @TestCase is present OR mirror
        // mode created a synthetic frame for Allure-only tests.
        if (testMethod.isAnnotationPresent(TestCase.class) || synthetic) {
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
