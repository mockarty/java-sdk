// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-local runtime state for the Mockarty test framework.
 *
 * <p>Tracks the currently bound TCM case + step stacks. Decorators
 * ({@link TestCase}, {@link Step}, {@link Scenario}) push/pop frames
 * here; {@link ru.mockarty.junit5.MockartyExtension} reads the active
 * frame after the test to ship a synthetic case-run.
 *
 * <p>The class is intentionally a thin data-holder — no SDK calls. That
 * keeps the framework testable without a live server and lets the
 * extension swap the upload backend (e.g. for a local fixture).
 */
public final class MockartyContext {

    /** Per-thread case stack (mirrors Python's ContextVar). */
    private static final ThreadLocal<Deque<CaseFrame>> CASE_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    /** Per-thread step stack. */
    private static final ThreadLocal<Deque<StepFrame>> STEP_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private MockartyContext() {}

    public static void pushCase(CaseFrame frame) {
        CASE_STACK.get().push(frame);
    }

    public static CaseFrame popCase() {
        Deque<CaseFrame> stack = CASE_STACK.get();
        return stack.isEmpty() ? null : stack.pop();
    }

    public static CaseFrame currentCase() {
        Deque<CaseFrame> stack = CASE_STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    public static void pushStep(StepFrame frame) {
        STEP_STACK.get().push(frame);
        CaseFrame caseFrame = currentCase();
        if (caseFrame != null) {
            caseFrame.steps.add(frame.snapshot());
            frame.slotIndex = caseFrame.steps.size() - 1;
        }
    }

    public static StepFrame popStep() {
        Deque<StepFrame> stack = STEP_STACK.get();
        if (stack.isEmpty()) {
            return null;
        }
        StepFrame frame = stack.pop();
        CaseFrame caseFrame = currentCase();
        if (caseFrame != null && frame.slotIndex >= 0
                && frame.slotIndex < caseFrame.steps.size()) {
            caseFrame.steps.set(frame.slotIndex, frame.snapshot());
        }
        return frame;
    }

    public static StepFrame currentStep() {
        Deque<StepFrame> stack = STEP_STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    /** Reset both stacks for the current thread. Used by the JUnit extension between tests. */
    public static void resetForTest() {
        CASE_STACK.remove();
        STEP_STACK.remove();
    }

    // ── Frame types ──────────────────────────────────────────────────

    /** One frame on the case stack — one {@link TestCase}-annotated test.
     *
     * <p>Phase 2.6 fields (description / expectedResult / customFields /
     * claimOwnership) carry the Mockarty-extended @TestCase metadata to
     * the upload bridge so the server's ExternalRunRequest can apply them
     * to the underlying TCM row. Allure adapters never set any of these.</p>
     */
    public static final class CaseFrame {
        public String caseId;
        public String caseName;
        public String planId;
        public boolean autoCreate;
        /** Markdown description for the TCM case row (Mockarty extension). */
        public String description;
        /** Markdown "what should happen" clause (Mockarty extension). */
        public String expectedResult;
        /** Custom fields list — each entry a {type,name,value} map. */
        public final List<Map<String, Object>> customFields = new ArrayList<>();
        /** When true, the receiver overwrites case fields on every upload. */
        public boolean claimOwnership;
        public final Map<String, Object> metadata = new HashMap<>();
        public final List<Map<String, Object>> attachments = new ArrayList<>();
        public final List<Map<String, Object>> steps = new ArrayList<>();

        public Map<String, Object> snapshot() {
            Map<String, Object> out = new HashMap<>();
            out.put("caseId", caseId);
            out.put("caseName", caseName);
            out.put("planId", planId);
            out.put("autoCreate", autoCreate);
            if (description != null) out.put("description", description);
            if (expectedResult != null) out.put("expectedResult", expectedResult);
            if (!customFields.isEmpty()) out.put("customFields", new ArrayList<>(customFields));
            if (claimOwnership) out.put("claimOwnership", true);
            out.put("metadata", new HashMap<>(metadata));
            out.put("attachments", Collections.unmodifiableList(attachments));
            out.put("steps", Collections.unmodifiableList(steps));
            return out;
        }
    }

    /** One frame on the step stack — one {@link Step} block. */
    public static final class StepFrame {
        public String name;
        public String status = "passed";
        public String error;
        public long startedNanos;
        public long durationNanos;
        public final Map<String, Object> metadata = new HashMap<>();
        /** Internal: index inside the parent case's steps list. */
        public int slotIndex = -1;

        public Map<String, Object> snapshot() {
            Map<String, Object> out = new HashMap<>();
            out.put("name", name);
            out.put("status", status);
            if (error != null) out.put("error", error);
            if (durationNanos > 0) out.put("durationNanos", durationNanos);
            if (!metadata.isEmpty()) out.put("metadata", new HashMap<>(metadata));
            return out;
        }
    }
}
