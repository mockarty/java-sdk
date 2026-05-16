// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import ru.mockarty.junit5.allure.AllureModel.Attachment;
import ru.mockarty.junit5.allure.AllureModel.Container;
import ru.mockarty.junit5.allure.AllureModel.Label;
import ru.mockarty.junit5.allure.AllureModel.Link;
import ru.mockarty.junit5.allure.AllureModel.Parameter;
import ru.mockarty.junit5.allure.AllureModel.Stage;
import ru.mockarty.junit5.allure.AllureModel.Status;
import ru.mockarty.junit5.allure.AllureModel.StatusDetails;
import ru.mockarty.junit5.allure.AllureModel.StepResult;
import ru.mockarty.junit5.allure.AllureModel.TestResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-thread Allure lifecycle for a single running test.
 *
 * <p>The lifecycle is intentionally thin: it tracks ONE active
 * {@link TestResult} per thread, plus an optional step stack. The JUnit /
 * TestNG / Cucumber adapters all funnel into the same API so a user who
 * runs hybrid suites (JUnit5 + Cucumber feature files) ends up with a
 * single, coherent {@code allure-results/} directory.</p>
 *
 * <h2>Thread model</h2>
 * <p>Each test thread keeps its own {@link Context} in a
 * {@link ThreadLocal}. Cross-thread step recording (rare: a test that
 * spawns helper threads expecting Allure context) is supported via
 * {@link #snapshotContext()} / {@link #bindContext(Context)}.</p>
 *
 * <h2>Container scope</h2>
 * <p>JUnit's {@code @BeforeAll}/{@code @AfterAll} run on a per-class
 * latch — the adapter pushes a {@link Container} the first time it sees
 * a test from a class and finalises it after the last test, so a class
 * with three tests produces three {@code -result.json} + one
 * {@code -container.json} that lists their UUIDs as {@code children}.
 * That matches allure-junit5's behaviour byte-for-byte.</p>
 */
public final class AllureLifecycle {

    private static final AllureLifecycle INSTANCE = new AllureLifecycle();

    /** Per-thread running state. */
    public static final class Context {
        public TestResult test;
        public final Deque<StepResult> stepStack = new ArrayDeque<>();

        public boolean hasTest() {
            return test != null;
        }

        public StepResult currentStep() {
            return stepStack.peek();
        }
    }

    private static final ThreadLocal<Context> CTX = ThreadLocal.withInitial(Context::new);

    /** uuid → Container, keyed by container UUID; flushed when finalised. */
    private final ConcurrentMap<String, Container> containers = new ConcurrentHashMap<>();

    /** Results dir resolved once per JVM. Hot-reload is rare and we'd
     * rather pay the cost of restart over a synchronized read on every step. */
    private final Path resultsDir;

    private AllureLifecycle() {
        this.resultsDir = AllureWriter.resolveResultsDirectory();
    }

    public static AllureLifecycle get() {
        return INSTANCE;
    }

    public Path resultsDirectory() {
        return resultsDir;
    }

    public Context context() {
        return CTX.get();
    }

    public Context snapshotContext() {
        return CTX.get();
    }

    public void bindContext(Context ctx) {
        if (ctx == null) {
            CTX.remove();
        } else {
            CTX.set(ctx);
        }
    }

    public void clearContext() {
        CTX.remove();
    }

    // ── Test lifecycle ──────────────────────────────────────────────────

    /** Begin a new test. Returns the newly-active TestResult. */
    public TestResult startTest(String name, String fullName) {
        TestResult t = new TestResult();
        t.uuid = UUID.randomUUID().toString();
        t.name = name;
        t.fullName = fullName;
        t.stage = Stage.RUNNING;
        t.start = System.currentTimeMillis();
        // historyId defaults to a stable hash of fullName + parameters so
        // re-runs collapse into the same history bucket. Adapters can
        // override it before stop().
        t.historyId = stableHistoryId(fullName, null);
        CTX.get().test = t;
        return t;
    }

    /** Stop the active test, write the result JSON, return the path. */
    public Path stopTest() {
        Context c = CTX.get();
        TestResult t = c.test;
        if (t == null) {
            return null;
        }
        try {
            // Defensive: if status wasn't set, bubble worst-of-steps.
            if (t.status == null) {
                t.status = Status.worst(t.steps);
            }
            t.stage = Stage.FINISHED;
            if (t.stop == 0) {
                t.stop = System.currentTimeMillis();
            }
            return AllureWriter.writeTestResult(resultsDir, t);
        } catch (IOException e) {
            // Fail-soft: never break a passing test because of disk IO.
            return null;
        } finally {
            c.test = null;
            c.stepStack.clear();
        }
    }

    /** Mark current test as failed with a Throwable. Status priority bubbles. */
    public void markFailed(Throwable t) {
        TestResult tr = CTX.get().test;
        if (tr == null) {
            return;
        }
        tr.status = classifyFailure(t);
        tr.statusDetails = throwableToDetails(t);
    }

    /** Mark current test as skipped (assumption failed). */
    public void markSkipped(String message) {
        TestResult tr = CTX.get().test;
        if (tr == null) {
            return;
        }
        tr.status = Status.SKIPPED;
        if (message != null) {
            StatusDetails sd = new StatusDetails();
            sd.message = message;
            tr.statusDetails = sd;
        }
    }

    public void markPassed() {
        TestResult tr = CTX.get().test;
        if (tr != null && tr.status == null) {
            tr.status = Status.PASSED;
        }
    }

    // ── Step lifecycle ──────────────────────────────────────────────────

    /** Open a step on the current test. Returns the step (caller closes via stopStep). */
    public StepResult startStep(String name) {
        Context c = CTX.get();
        StepResult s = new StepResult();
        s.name = name;
        s.stage = Stage.RUNNING;
        s.start = System.currentTimeMillis();
        StepResult parent = c.currentStep();
        if (parent != null) {
            parent.steps.add(s);
        } else if (c.test != null) {
            c.test.steps.add(s);
        }
        c.stepStack.push(s);
        return s;
    }

    public void stopStep(boolean passed) {
        Context c = CTX.get();
        StepResult s = c.stepStack.poll();
        if (s == null) {
            return;
        }
        s.stage = Stage.FINISHED;
        s.stop = System.currentTimeMillis();
        if (s.status == null) {
            s.status = passed ? Status.PASSED : Status.FAILED;
        }
    }

    public void stopStepFailed(Throwable cause) {
        Context c = CTX.get();
        StepResult s = c.stepStack.poll();
        if (s == null) {
            return;
        }
        s.stage = Stage.FINISHED;
        s.stop = System.currentTimeMillis();
        s.status = classifyFailure(cause);
        s.statusDetails = throwableToDetails(cause);
    }

    // ── Attachment ──────────────────────────────────────────────────────

    /** Register an attachment on the active step (preferred) or test. */
    public Attachment attach(String name, byte[] body, String mime) {
        try {
            String source = AllureWriter.writeAttachment(resultsDir, name,
                    body == null ? new byte[0] : body, mime);
            Attachment a = new Attachment(name, source, mime);
            Context c = CTX.get();
            StepResult step = c.currentStep();
            if (step != null) {
                step.attachments.add(a);
            } else if (c.test != null) {
                c.test.attachments.add(a);
            }
            return a;
        } catch (IOException e) {
            return null;
        }
    }

    public Attachment attachJson(String name, String json) {
        return attach(name, json == null ? null : json.getBytes(StandardCharsets.UTF_8),
                "application/json");
    }

    public Attachment attachText(String name, String text) {
        return attach(name, text == null ? null : text.getBytes(StandardCharsets.UTF_8),
                "text/plain; charset=utf-8");
    }

    public Attachment attachPng(String name, byte[] png) {
        return attach(name, png, "image/png");
    }

    public Attachment attachBinary(String name, byte[] body, String mime) {
        return attach(name, body, mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
    }

    // ── Metadata helpers (called by reflection harvest + user code) ─────

    public void addLabel(String name, String value) {
        TestResult tr = CTX.get().test;
        if (tr != null && name != null && value != null) {
            tr.labels.add(new Label(name, value));
        }
    }

    public void addLink(String name, String url, String type) {
        TestResult tr = CTX.get().test;
        if (tr != null) {
            tr.links.add(new Link(name, url, type));
        }
    }

    public void addParameter(String name, String value) {
        TestResult tr = CTX.get().test;
        if (tr != null && name != null) {
            tr.parameters.add(new Parameter(name, value == null ? "" : value));
        }
    }

    public void addParameter(Parameter param) {
        TestResult tr = CTX.get().test;
        if (tr != null && param != null) {
            tr.parameters.add(param);
        }
    }

    public void setDescription(String text) {
        TestResult tr = CTX.get().test;
        if (tr != null) {
            tr.description = text;
        }
    }

    public void setDescriptionHtml(String html) {
        TestResult tr = CTX.get().test;
        if (tr != null) {
            tr.descriptionHtml = html;
        }
    }

    public void setHistoryId(String historyId) {
        TestResult tr = CTX.get().test;
        if (tr != null && historyId != null && !historyId.isEmpty()) {
            tr.historyId = historyId;
        }
    }

    // ── Container lifecycle ─────────────────────────────────────────────

    /** Begin a Container scope; returns the container UUID for cross-test wiring. */
    public String startContainer(String name) {
        Container c = new Container();
        c.uuid = UUID.randomUUID().toString();
        c.name = name;
        c.start = System.currentTimeMillis();
        containers.put(c.uuid, c);
        return c.uuid;
    }

    /** Register a child test on a container (call right after startTest). */
    public void addContainerChild(String containerUuid, String testUuid) {
        Container c = containers.get(containerUuid);
        if (c != null && testUuid != null) {
            c.children.add(testUuid);
        }
    }

    /** Finalise + flush a container. Returns the result file path. */
    public Path stopContainer(String containerUuid) {
        Container c = containers.remove(containerUuid);
        if (c == null) {
            return null;
        }
        c.stop = System.currentTimeMillis();
        try {
            return AllureWriter.writeContainer(resultsDir, c);
        } catch (IOException e) {
            return null;
        }
    }

    /** Append a before-fixture step to a container. */
    public StepResult containerBefore(String containerUuid, String name) {
        return containerFixture(containerUuid, name, true);
    }

    public StepResult containerAfter(String containerUuid, String name) {
        return containerFixture(containerUuid, name, false);
    }

    private StepResult containerFixture(String containerUuid, String name, boolean before) {
        Container c = containers.get(containerUuid);
        if (c == null) {
            return null;
        }
        StepResult s = new StepResult();
        s.name = name;
        s.stage = Stage.FINISHED;
        s.start = System.currentTimeMillis();
        s.stop = s.start;
        s.status = Status.PASSED;
        (before ? c.befores : c.afters).add(s);
        return s;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /** AssertionError-shaped failures map to FAILED, anything else to BROKEN. */
    static Status classifyFailure(Throwable t) {
        if (t == null) {
            return Status.FAILED;
        }
        // org.opentest4j.AssertionFailedError and java AssertionError → FAILED.
        // TestNG and Hamcrest also throw AssertionError-derived; same handling.
        Throwable cause = t;
        while (cause != null) {
            String fqn = cause.getClass().getName();
            if (fqn.equals("org.opentest4j.AssertionFailedError")
                    || fqn.equals("java.lang.AssertionError")
                    || fqn.endsWith("AssertionError")
                    || fqn.endsWith("AssertionFailedError")
                    || fqn.endsWith("ComparisonFailure")) {
                return Status.FAILED;
            }
            cause = cause.getCause();
        }
        return Status.BROKEN;
    }

    static StatusDetails throwableToDetails(Throwable t) {
        StatusDetails sd = new StatusDetails();
        if (t == null) {
            return sd;
        }
        sd.message = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
        }
        sd.trace = sw.toString();
        return sd;
    }

    /** Stable hash for the historyId. Same {@code fullName} + same {@code paramSig}
     * always produce the same id — enables Allure to collapse retries. */
    public static String stableHistoryId(String fullName, String paramSig) {
        String src = (fullName == null ? "" : fullName)
                + "|" + (paramSig == null ? "" : paramSig);
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] dig = md.digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            // MD5 is guaranteed by JLS; fall back to Java hash to keep going.
            return Integer.toHexString(src.hashCode());
        }
    }
}
