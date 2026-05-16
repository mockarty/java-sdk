// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.testcontainers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** Trivial coverage of the exception type so the public surface stays
 * non-zero-covered in the JaCoCo report. */
class MockartyContainerExceptionTest {

    @Test
    void messageOnlyConstructor() {
        MockartyContainerException ex = new MockartyContainerException("boom");
        assertEquals("boom", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void messageAndCauseConstructor() {
        RuntimeException cause = new RuntimeException("inner");
        MockartyContainerException ex = new MockartyContainerException("wrap", cause);
        assertEquals("wrap", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
