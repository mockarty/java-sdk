// Copyright (c) 2026 Mockarty. All rights reserved.
// SPDX-License-Identifier: MIT

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
