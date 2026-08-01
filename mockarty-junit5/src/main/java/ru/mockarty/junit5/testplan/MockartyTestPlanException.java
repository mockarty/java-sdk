// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.junit5.testplan;

/**
 * Thrown when an Allure test plan was requested but cannot be honoured.
 *
 * <p>Raised for a missing, unreadable or malformed plan, and for a plan that
 * selects no tests at all. It is deliberately fatal: the alternative — the
 * reference adapters' silent fallback to a full run — turns "re-run my 3
 * failed tests" into "run all 3000 and report green", which is the failure
 * this package exists to prevent.</p>
 *
 * <p>It is never thrown when no plan is configured; that is an ordinary
 * unfiltered run.</p>
 */
public class MockartyTestPlanException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message what was wrong with the plan, and why the run stopped.
     */
    public MockartyTestPlanException(String message) {
        super(message);
    }

    /**
     * @param message what was wrong with the plan, and why the run stopped.
     * @param cause   the underlying I/O or parse failure.
     */
    public MockartyTestPlanException(String message, Throwable cause) {
        super(message, cause);
    }
}
