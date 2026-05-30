// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.framework;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for {@link MockartyContext} stack semantics. */
class MockartyContextTest {

    @AfterEach
    void cleanup() {
        MockartyContext.resetForTest();
    }

    @Test
    void pushPopCase_yieldsLifoOrder() {
        assertNull(MockartyContext.currentCase());

        MockartyContext.CaseFrame outer = new MockartyContext.CaseFrame();
        outer.caseId = "OUTER";
        MockartyContext.pushCase(outer);

        MockartyContext.CaseFrame inner = new MockartyContext.CaseFrame();
        inner.caseId = "INNER";
        MockartyContext.pushCase(inner);

        assertEquals("INNER", MockartyContext.currentCase().caseId);
        MockartyContext.popCase();
        assertEquals("OUTER", MockartyContext.currentCase().caseId);
        MockartyContext.popCase();
        assertNull(MockartyContext.currentCase());
    }

    @Test
    void stepRecordsOnActiveCase_andUpdatesCorrectSlotOnNestedPop() {
        MockartyContext.CaseFrame caseFrame = new MockartyContext.CaseFrame();
        MockartyContext.pushCase(caseFrame);

        MockartyContext.StepFrame outer = new MockartyContext.StepFrame();
        outer.name = "outer";
        MockartyContext.pushStep(outer);

        MockartyContext.StepFrame inner = new MockartyContext.StepFrame();
        inner.name = "inner";
        MockartyContext.pushStep(inner);

        // Both steps captured in order
        assertEquals(2, caseFrame.steps.size());
        assertEquals("outer", caseFrame.steps.get(0).get("name"));
        assertEquals("inner", caseFrame.steps.get(1).get("name"));

        // Mutate inner status, then pop — must update slot 1, not slot 0
        inner.status = "failed";
        inner.error = "boom";
        MockartyContext.popStep();
        assertEquals("failed", caseFrame.steps.get(1).get("status"));
        assertEquals("outer", caseFrame.steps.get(0).get("name"));
        assertEquals("passed", caseFrame.steps.get(0).get("status"));

        // Pop outer — slot 0 must update, slot 1 (inner) preserved
        outer.status = "passed";
        MockartyContext.popStep();
        assertEquals("failed", caseFrame.steps.get(1).get("status"),
                "popping outer must NOT overwrite inner slot");

        MockartyContext.popCase();
    }

    @Test
    void stepOutsideCase_doesNotThrow() {
        MockartyContext.StepFrame frame = new MockartyContext.StepFrame();
        frame.name = "orphan";
        MockartyContext.pushStep(frame);
        assertNotNull(MockartyContext.currentStep());
        MockartyContext.popStep();
        assertNull(MockartyContext.currentStep());
    }

    @Test
    void resetForTest_clearsBothStacks() {
        MockartyContext.pushCase(new MockartyContext.CaseFrame());
        MockartyContext.pushStep(new MockartyContext.StepFrame());
        assertNotNull(MockartyContext.currentCase());
        assertNotNull(MockartyContext.currentStep());
        MockartyContext.resetForTest();
        assertNull(MockartyContext.currentCase());
        assertNull(MockartyContext.currentStep());
    }
}
