// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.tester;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage for {@link Tester#wrap(String, Runnable)} — the ergonomic
 * brought in to match Go's {@code Tester.Wrap} and Python's
 * {@code mockarty.tester.wrap()}. Synthetic "wrap" StepRecord is
 * emitted; the lambda runs between two {@code flushPending()} calls
 * so any pending chain commits before and after.
 */
class TesterWrapTest {

    @Test
    void wrapAppendsSyntheticStepRecord() {
        try (Tester t = new Tester.Builder().build()) {
            t.wrap("login flow", () -> {
                // empty body — we are only proving the marker fires.
            });
            List<StepRecord> steps = t.report();
            assertEquals(1, steps.size(), "expected exactly one synthetic step");
            StepRecord m = steps.get(0);
            assertEquals("wrap", m.protocol);
            assertEquals("login flow", m.name);
            assertNotNull(m.startedAt);
            assertNotNull(m.endedAt);
            // endedAt must not be earlier than startedAt — basic temporal sanity.
            assertFalse(m.endedAt.isBefore(m.startedAt),
                    "endedAt before startedAt: " + m.startedAt + " → " + m.endedAt);
        }
    }

    @Test
    void wrapReturnsSameTesterForFluentChaining() {
        try (Tester t = new Tester.Builder().build()) {
            Tester ret = t.wrap("step", () -> {});
            assertSame(t, ret, "wrap() must return the same Tester");
        }
    }

    @Test
    void wrapWithNullBodyIsNoOpButFlushesPending() {
        try (Tester t = new Tester.Builder().build()) {
            // Even with a null body the wrap call should flush any
            // pending chain (none here, but the call must succeed).
            t.wrap("noop", null);
            // No synthetic marker is appended when body is null because
            // the wrap didn't actually run — keeps reports clean.
            assertEquals(0, t.report().size(), "null body must not append a synthetic marker");
        }
    }

    @Test
    void wrapBodyRunsExactlyOnce() {
        AtomicInteger calls = new AtomicInteger();
        try (Tester t = new Tester.Builder().build()) {
            t.wrap("counter", calls::incrementAndGet);
        }
        assertEquals(1, calls.get(), "body should run exactly once");
    }

    @Test
    void wrapPropagatesPanicAfterFlush() {
        AtomicBoolean ranAfter = new AtomicBoolean(false);
        try (Tester t = new Tester.Builder().build()) {
            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                t.wrap("explodes", () -> {
                    throw new RuntimeException("boom");
                });
            });
            assertEquals("boom", thrown.getMessage());
            // After the panic the synthetic marker MUST still be
            // appended (finally block did its job).
            ranAfter.set(true);
            assertEquals(1, t.report().size(),
                    "marker must be appended even when body throws");
            assertEquals("wrap", t.report().get(0).protocol);
        }
        assertTrue(ranAfter.get());
    }

    @Test
    void nestedWrapAppendsMultipleMarkers() {
        try (Tester t = new Tester.Builder().build()) {
            t.wrap("outer", () -> t.wrap("inner", () -> {}));
            List<StepRecord> steps = t.report();
            // The current model is flat (no parent/child link). Both
            // markers land in the report. The inner one finishes first
            // and is appended first; the outer one finishes second.
            assertEquals(2, steps.size());
            assertEquals("inner", steps.get(0).name);
            assertEquals("outer", steps.get(1).name);
        }
    }

    @Test
    void wrapAroundEmptyChainStillFlushes() {
        // Regression guard: a wrap() call with no HTTP / GraphQL / etc.
        // inside it must not throw or leak pending state.
        try (Tester t = new Tester.Builder().build()) {
            t.wrap("empty", () -> {});
            assertTrue(t.ok(), "ok() should return true after an empty wrap");
        }
    }
}
