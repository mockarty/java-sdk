// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

/**
 * Mockarty TCM step — open a named block whose status is recorded on the
 * surrounding case frame. Mirrors {@code mockarty.testing.step} in the
 * Python SDK; same semantics across both languages.
 *
 * <p>Three usage shapes — pick the one that reads cleanly in your test.
 * All three record an identical step on the case frame.</p>
 *
 * <pre>{@code
 * // 1) try-with-resources (clearest for inline blocks):
 * try (Step s = Step.open("login form submit")) {
 *     submitForm(...);
 * }
 *
 * // 2) supplier wrapper (when you want a return value):
 * String token = Step.run("issue token", () -> auth.issueToken(...));
 *
 * // 3) runnable wrapper (no return value):
 * Step.run("verify headers", () -> { assertHeaders(response); });
 * }</pre>
 *
 * <p>Steps nest. Exceptions are captured with status="failed" and the
 * exception is rethrown so the test still fails. When {@code allure-junit5}
 * is on the classpath each step is mirrored to an Allure step
 * automatically (best-effort — silent no-op if missing).</p>
 */
public final class Step implements AutoCloseable {

    private final MockartyContext.StepFrame frame;
    /** Optional opaque token for the parallel Allure step opened by the
     * mirror bridge. {@code null} when Allure is not on the classpath. */
    private final Object allureToken;
    private boolean closed;

    private Step(String name) {
        this.frame = new MockartyContext.StepFrame();
        this.frame.name = name;
        this.frame.startedNanos = System.nanoTime();
        MockartyContext.pushStep(this.frame);
        // Mirror into Allure if the runtime is on the classpath. Silent
        // no-op otherwise — matches the docstring promise on this class.
        this.allureToken = AllureMirror.beginAllureStep(name);
        // Always mirror into the Mockarty AllureLifecycle (it writes the
        // <uuid>-result.json — present regardless of allure-java-commons).
        ru.mockarty.junit5.allure.AllureLifecycle.get().startStep(name);
    }

    /** Open a step in try-with-resources style. */
    public static Step open(String name) {
        validate(name);
        return new Step(name);
    }

    /** Run a {@link Runnable} inside a step block. Captures throw status, rethrows. */
    public static void run(String name, Runnable body) {
        validate(name);
        Step s = new Step(name);
        try {
            body.run();
        } catch (RuntimeException e) {
            s.markFailed(e);
            throw e;
        } catch (Error e) {
            s.markFailed(e);
            throw e;
        } finally {
            s.close();
        }
    }

    /** Run a {@link ThrowingSupplier} inside a step block and return its value. */
    public static <T> T run(String name, ThrowingSupplier<T> body) {
        validate(name);
        Step s = new Step(name);
        try {
            return body.get();
        } catch (RuntimeException e) {
            s.markFailed(e);
            throw e;
        } catch (Exception e) {
            s.markFailed(e);
            throw new RuntimeException(e);
        } finally {
            s.close();
        }
    }

    /** Attach metadata to the active step. No-op outside a step block. */
    public Step withMetadata(String key, Object value) {
        if (frame != null) {
            frame.metadata.put(key, value);
        }
        return this;
    }

    /** Mark the active step as failed. Useful inside try-with-resources blocks. */
    public Step markFailed(Throwable cause) {
        if (frame != null) {
            frame.status = "failed";
            frame.error = cause == null
                    ? "failed"
                    : cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return this;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (frame != null) {
            frame.durationNanos = System.nanoTime() - frame.startedNanos;
        }
        MockartyContext.popStep();
        boolean passed = frame == null || !"failed".equals(frame.status);
        // Close the mirrored Allure step (if one was opened via runtime bridge).
        if (allureToken != null) {
            AllureMirror.endAllureStep(allureToken, passed);
        }
        // Close the mockarty AllureLifecycle step.
        ru.mockarty.junit5.allure.AllureLifecycle.get().stopStep(passed);
    }

    private static void validate(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Step.open/run requires a non-empty name");
        }
    }

    /** Functional interface for {@link #run(String, ThrowingSupplier)}. */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
