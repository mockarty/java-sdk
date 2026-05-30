// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Behaviour tests for {@link Step} factory + try-with-resources lifecycle. */
class StepTest {

    private MockartyContext.CaseFrame caseFrame;

    @BeforeEach
    void setUp() {
        caseFrame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(caseFrame);
    }

    @AfterEach
    void cleanup() {
        MockartyContext.resetForTest();
    }

    @Test
    void open_recordsStepOnActiveCase() {
        try (Step ignored = Step.open("login form")) {
            assertNotNull(MockartyContext.currentStep());
        }
        assertNull(MockartyContext.currentStep());
        assertEquals(1, caseFrame.steps.size());
        assertEquals("login form", caseFrame.steps.get(0).get("name"));
        assertEquals("passed", caseFrame.steps.get(0).get("status"));
    }

    @Test
    void runRunnable_capturesFailedStatusAndRethrows() {
        RuntimeException raised = assertThrows(RuntimeException.class,
                () -> Step.run("risky", () -> { throw new RuntimeException("boom"); }));
        assertEquals("boom", raised.getMessage());
        assertEquals(1, caseFrame.steps.size());
        assertEquals("failed", caseFrame.steps.get(0).get("status"));
        assertTrue(((String) caseFrame.steps.get(0).get("error")).contains("boom"));
    }

    @Test
    void runSupplier_returnsValueAndRecordsPassed() {
        String returned = Step.run("issue token", () -> "abc");
        assertEquals("abc", returned);
        assertEquals(1, caseFrame.steps.size());
        assertEquals("passed", caseFrame.steps.get(0).get("status"));
    }

    @Test
    void open_rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> Step.open(""));
        assertThrows(IllegalArgumentException.class, () -> Step.open("   "));
    }

    @Test
    void run_rejectsNullName() {
        assertThrows(IllegalArgumentException.class, () -> Step.run(null, () -> {}));
    }

    @Test
    void markFailed_setsStatusInsideTryWithResources() {
        try (Step s = Step.open("verify")) {
            try {
                throw new IllegalStateException("oops");
            } catch (IllegalStateException e) {
                s.markFailed(e);
                // swallowed for test purposes — real test would rethrow
            }
        }
        assertEquals("failed", caseFrame.steps.get(0).get("status"));
        assertTrue(((String) caseFrame.steps.get(0).get("error")).contains("oops"));
    }
}
