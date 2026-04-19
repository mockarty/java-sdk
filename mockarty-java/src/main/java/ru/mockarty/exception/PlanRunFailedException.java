// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.exception;

/**
 * Thrown by {@code TestPlanApi.waitForRun} when a run finishes in the
 * {@code failed} state.
 */
public class PlanRunFailedException extends MockartyException {

    public PlanRunFailedException(String message) {
        super(message);
    }
}
